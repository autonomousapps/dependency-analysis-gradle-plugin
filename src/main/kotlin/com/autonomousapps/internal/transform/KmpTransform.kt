// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind

internal class KmpTransform(
  coordinates: Coordinates,
  declarations: Set<Declaration>,
  explicitSourceSets: Set<String>,
  configurationNames: ConfigurationNames,
  buildPath: String,
  dependencyGraph: Map<String, DependencyGraphView>,
  isKaptApplied: Boolean,
) : AbstractTransform(
  coordinates = coordinates,
  declarations = declarations,
  explicitSourceSets = explicitSourceSets,
  configurationNames = configurationNames,
  dependencyGraph = dependencyGraph,
  buildPath = buildPath,
  isKaptApplied = isKaptApplied,
) {

  // TODO(tsr): ultimately I want to replace the original reduce function with something much simpler and more tailored
  //  to the KMP use-case.
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    val reducedUsages = reduceUsages(usages.toMutableSet())
    computeAdvice(advice, reducedUsages, declarations.toMutableSet())

    return simplify(advice)
  }

  /** Reduce usages to fewest possible (1+). */
  private fun reduceUsages(usages: MutableSet<Usage>): MutableSet<Usage> {
    if (usages.isEmpty()) return usages

    val kinds = usages.mapToSet { it.sourceKind.kind }
    check(kinds.size == 1) { "Expected a single ${SourceKind::class.java.simpleName}. Got: $kinds" }

    // This could be a JVM module or an Android module only analyzing a singe variant. For the latter, we need to
    // transform it into a "main" variant.
    return if (usages.size == 1) {
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        sourceKind = usage.sourceKind.base(),
        bucket = usage.bucket,
        reasons = usage.reasons,
      ).intoMutableSet()
    } else if (!isSingleBucketForSingleVariant(usages)) {
      val usagesBySourceSet = mutableMapOf<String, Usage>()
      usages.forEach { usage ->
        usagesBySourceSet.merge(usage.sourceKind.name, usage) { acc, inc ->
          Usage(
            buildType = null,
            flavor = null,
            // TODO(tsr): this is lossy. Maybe the solution is to craft an artificial classpath for 'commonX' SourceKinds.
            //  `acc` might have compileClasspathName=androidCompileClasspath, runtimeClasspathName=androidRuntimeClasspath
            //  `inc` might have compileClasspathName=jvmCompileClasspath, runtimeClasspathName=jvmRuntimeClasspath
            sourceKind = acc.sourceKind,
            bucket = acc.bucket.highest(inc.bucket),
            reasons = acc.reasons.plus(inc.reasons)
          )
        }
      }

      usagesBySourceSet.values.toMutableSet()
    } else {
      // More than one usage, but all in the same bucket with the same variant. We reduce the usages to a single usage.
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        // TODO(tsr): this is lossy. There isn't a true "base" concept in KMP.
        sourceKind = usage.sourceKind.base(),
        bucket = usage.bucket,
        reasons = usages.flatMapToSet { it.reasons },
      ).intoMutableSet()
    }
  }

  /** Turn usage information into actionable advice. */
  private fun computeAdvice(
    advice: MutableSet<Advice>,
    usages: MutableSet<Usage>,
    declarations: MutableSet<Declaration>,
  ) {
    val usageIter = usages.iterator()
    while (usageIter.hasNext()) {
      val usage = usageIter.next()
      val declarationsForVariant = declarations.filterToSet { declaration ->
        usage.sourceKind.name == DependencyScope.sourceSetName(declaration.configurationName)
      }

      // We have a declaration on the same variant as the usage. Remove or change it, if necessary.
      if (declarationsForVariant.isNotEmpty()) {
        // drain
        declarations.removeAll(declarationsForVariant)
        usageIter.remove()

        declarationsForVariant.forEach { decl ->
          if (
            usage.bucket == Bucket.NONE
            // Don't remove an undeclared usage (this would make no sense)
            && Reason.Undeclared !in usage.reasons
            // Don't remove a declaration on compileOnly, compileOnlyApi, providedCompile
            && decl.bucket(configurationNames) != Bucket.COMPILE_ONLY
            // Don't remove a declaration on runtimeOnly
            && decl.bucket(configurationNames) != Bucket.RUNTIME_ONLY
          ) {
            advice += Advice.ofRemove(
              coordinates = declarationCoordinates(decl),
              declaration = decl
            )
          } else if (
            usage.bucket != Bucket.NONE
            // Don't change a match, it's correct!
            && !usage.bucket.matches(decl, configurationNames)
            // Don't change a declaration on compileOnly, compileOnlyApi, providedCompile
            && decl.bucket(configurationNames) != Bucket.COMPILE_ONLY
            // Don't change a declaration on runtimeOnly
            && decl.bucket(configurationNames) != Bucket.RUNTIME_ONLY
          ) {
            advice += Advice.ofChange(
              coordinates = declarationCoordinates(decl),
              fromConfiguration = decl.configurationName,
              toConfiguration = mapper.toConfiguration(usage)
            )
          }
        }
      }
    }

    // Any remaining usages should be added
    addRemainingUsages(usages, advice)

    // Any remaining declarations should be removed
    removeRemainingDeclarations(declarations, advice)
  }

  /**
   * Simply advice by transforming matching pairs of add-advice and remove-advice into a single change-advice. Also
   * strips out confusing and verbose advice to change/add dependencies for the same product flavor.
   *
   * In addition, strip advice that would add redundant declarations in related source sets, or which would upgrade test
   * dependencies.
   */
  private fun simplify(advice: MutableSet<Advice>): Set<Advice> {
    // TODO(tsr): This top part is identical to JvmTransform
    val (add, remove) = advice.mutPartitionOf(
      { it.isAdd() || it.isCompileOnly() },
      { it.isRemove() || it.isRemoveCompileOnly() },
    )

    add.forEach { theAdd ->
      remove
        .find { theRemove -> theRemove.coordinates == theAdd.coordinates }
        ?.let { theRemove ->
          // nb: this code block is duplicated exactly above.
          // Replace add + remove => change.
          advice -= theAdd
          advice -= theRemove
          remove -= theRemove

          advice += Advice.ofChange(
            coordinates = theRemove.coordinates,
            fromConfiguration = theRemove.fromConfiguration!!,
            toConfiguration = theAdd.toConfiguration!!
          )
        }
    }

    // reduce "add to commonMainApi and add to commonTestImplementation" to "add to commonMainApi"
    if (add.any { it.toConfiguration!!.startsWith(KmpSourceKind.COMMON_MAIN_NAME) }) {
      advice.removeIf { it.toConfiguration?.startsWith(KmpSourceKind.COMMON_TEST_NAME) == true }
    }

    // In some cases, a dependency might be non-transitive but still not be "declared" in a build script. For example, a
    // custom source set could extend another source set. In such a case, we don't want to suggest a user declare that
    // dependency. We can detect this by looking at the dependency graph related to the given source set.
    // if on some add-advice...
    // ...the fromConfiguration == null and toConfiguration == functionalTestApi (for example),
    // ...and if the dependency graph contains the dependency with a node at functionalTest directly from the root,
    // => we need to remove that advice.

    return advice.asSequence()
      .filterNot { isDeclaredInRelatedSourceSet(advice, it) }
      .map { downgradeTestDependencies(it) }
      .toSet()
  }
}
