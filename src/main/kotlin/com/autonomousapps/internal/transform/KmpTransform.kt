// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.transform

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind

internal class KmpTransform(
  private val declarations: Set<Declaration>,
  coordinates: Coordinates,
  explicitSourceSets: Set<String> = emptySet(),
  configurationNames: ConfigurationNames,
  buildPath: String,
  dependencyGraph: Map<String, DependencyGraphView>,
  isKaptApplied: Boolean = false,
) : AbstractTransform(
  coordinates = coordinates,
  configurationNames = configurationNames,
  dependencyGraph = dependencyGraph,
  buildPath = buildPath,
  explicitSourceSets = explicitSourceSets,
  isKaptApplied = isKaptApplied,
) {

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    var (mainUsages, testUsages, androidTestFixturesUsages, androidTestUsages, customJvmUsage) = usages.mutPartitionOf(
      { it.sourceKind.kind == SourceKind.MAIN_KIND },
      { it.sourceKind.kind == SourceKind.TEST_KIND },
      { it.sourceKind.kind == SourceKind.ANDROID_TEST_FIXTURES_KIND },
      { it.sourceKind.kind == SourceKind.ANDROID_TEST_KIND },
      { it.sourceKind.kind == SourceKind.CUSTOM_JVM_KIND },
    )

    val hasCustomSourceSets = hasCustomSourceSets(usages)
    val (mainDeclarations, testDeclarations, androidTestFixturesDeclarations, androidTestDeclarations, customJvmDeclarations) =
      declarations.mutPartitionOf(
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.MAIN_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.TEST_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.ANDROID_TEST_FIXTURES_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.ANDROID_TEST_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.CUSTOM_JVM_KIND },
      )

    // Notes on "visibility":
    // 1. We care about _usage_ because usage is what matters for final state of dependency declarations.
    // 2. Unfortunately, usage isn't good enough. Something might have a bucket=IMPL usage but declaration=compileOnly,
    //    and because we don't change compileOnly declarations, the dependency won't be visible on downstream runtime
    //    classpaths.
    // 3. I know all the classpaths something is CURRENTLY visible on via `dependenciesToClasspaths`, but that's based
    //    on current declarations that may change. We need to know USAGE, DECLARATION (because of special handling)
    // 4. Declarations aren't good enough, because I know something _will be_ declared on runtimeOnly, even if it isn't
    //    currently. I suppose I can say that the runtime classpath has special handling. Our advice specifically trims
    //    the compile classpath based on detected usage in the bytecode. We also already suggest moving things to
    //    runtimeOnly if there's no detected compile-time usage, but the thing has runtime capabilities. Now we want to
    //    say, _add_ this thing to runtimeOnly, if it has runtime capabilities.
    val visibility = Bucket.determineVisibility(mainUsages, mainDeclarations, configurationNames)

    /*
     * Main usages.
     */

    val singleVariant = mainUsages.size == 1
    mainUsages = reduceUsages(mainUsages)
    computeAdvice(advice, mainUsages, mainDeclarations, singleVariant)

    /*
     * Test usages.
     */

    // If main usages are visible downstream, then we don't need a test declaration
    testUsages = testUsages.simplify(visibility, SourceKind.TEST_NAME)
    computeAdvice(advice, testUsages, testDeclarations, testUsages.size == 1)

    /*
     * Android test fixtures usages.
     */
    androidTestFixturesUsages = reduceUsages(androidTestFixturesUsages)
    computeAdvice(
      advice,
      androidTestFixturesUsages,
      androidTestFixturesDeclarations,
      androidTestFixturesUsages.size == 1
    )

    /*
     * Android test usages.
     */

    androidTestUsages = androidTestUsages.simplify(visibility, SourceKind.ANDROID_TEST_NAME)
    computeAdvice(advice, androidTestUsages, androidTestDeclarations, androidTestUsages.size == 1)

    /*
     * Custom JVM source sets like 'testFixtures', 'integrationTest' or other custom source sets and feature variants
     */

    customJvmUsage = reduceUsages(customJvmUsage)
    computeAdvice(advice, customJvmUsage, customJvmDeclarations, customJvmUsage.size == 1, true)

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
      // More than one usage _and_ multiple buckets: in a variant situation (Android), there are no "main" usages, by
      // definition. Everything is debugImplementation, releaseApi, etc. If each variant has a different usage, we
      // respect that. In JVM, each (feature) variant is distinct.
      usages
    } else {
      // More than one usage, but all in the same bucket with the same variant. We reduce the usages to a single usage.
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        sourceKind = usage.sourceKind.base(),
        bucket = usage.bucket,
        reasons = usages.flatMapToSet { it.reasons },
      ).intoMutableSet()
    }
  }

  /**
   * Simplifies a set of [usages][Usage], taking into account the [visibility] of [sourceSetName] into the `main` source
   * set, and also whether the user has opted-into "explicit source sets." For example, if a `main` usage is visible
   * on the compile classpath of a downstream source set like `test`, then we filter out all non-runtime-only usages.
   *
   * Android unit (host) tests get special treatment, essentially collapsing something like "debugTest" into simply
   * "test," reflecting the reality of how most Android projects are configured.
   */
  private fun MutableSet<Usage>.simplify(
    visibility: Bucket.Visibility,
    sourceSetName: String,
  ): MutableSet<Usage> {
    return if (visibility.forEither && !explicitFor(sourceSetName)) {
      // TODO(tsr): add test for this case.
      //  1. a dep that should be moved from impl->api
      //  2. dep has runtime capabilities as well and is on test runtime classpath
      //  3. previously we'd see advice to both move to api and also add to testRuntimeOnly. The latter is undesirable.

      // Yes, if something is visible forCompile and forRuntime, then we filter out all usages.
      var seq = asSequence()
      if (visibility.forCompile) {
        seq = seq.filterNot { usage -> usage.bucket != Bucket.RUNTIME_ONLY }
      }
      if (visibility.forRuntime) {
        seq = seq.filterNot { usage -> usage.bucket == Bucket.RUNTIME_ONLY }
      }

      seq.toMutableSet()
    } else {
      reduceUsages(this)
    }
  }

  /** Turn usage information into actionable advice. */
  private fun computeAdvice(
    advice: MutableSet<Advice>,
    usages: MutableSet<Usage>,
    declarations: MutableSet<Declaration>,
    singleVariant: Boolean,
    pureJvmVariant: Boolean = false
  ) {
    val usageIter = usages.iterator()
    val hasCustomSourceSets = hasCustomSourceSets(usages)
    while (usageIter.hasNext()) {
      val usage = usageIter.next()
      val declarationsForVariant = declarations.filterToSet { declaration ->
        usage.sourceKind == declaration.findSourceKind(hasCustomSourceSets)
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
      } else if (!pureJvmVariant) {
        // No exact match, so look for a declaration on the same bucket
        // (e.g., usage is 'api' and declaration is 'debugApi').

        // This code path does not apply for pure Java feature variants (source sets).
        // For example 'api' and 'testFixturesApi' are completely separated variants
        // and suggesting to move dependencies between them can lead to confusing results.
        // Exception are the 'main' and 'test' source sets which are handled special
        // because 'testImplementation' extends from 'implementation' and we allow moving
        // dependencies from 'testImplementation' to 'implementation'. See also:
        // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/900

        declarations
          .find { usage.bucket.matches(it, configurationNames) }
          ?.let { theDecl ->
            // drain
            declarations.remove(theDecl)
            usageIter.remove()

            // Don't change a single-usage match, it's correct!
            if (!(singleVariant && usage.bucket.matches(theDecl, configurationNames))) {
              advice += Advice.ofChange(
                coordinates = declarationCoordinates(theDecl),
                fromConfiguration = theDecl.configurationName,
                toConfiguration = mapper.toConfiguration(usage)
              )
            }
          }
      }
    }

    // Any remaining usages should be added
    usages.asSequence()
      // Don't add unused usages!
      .filterUsed()
      // Don't add runtimeOnly or compileOnly (compileOnly, compileOnlyApi, providedCompile) declarations
      // nb: this probably remains the correct choice, but it can lead to issues when we remove an "unused" dependency
      // and fail to add a required runtimeOnly dependency that was part of the unused dep's transitive graph. Removing
      // this line would lead to "super strict" declarations that are, perhaps, more bazel-like (every classpath
      // consists only of positively-declared dependencies).
      .filterNot { usage -> usage.bucket == Bucket.COMPILE_ONLY }
      // Don't add something that is only present on the compileClasspath as that will change the runtimeClasspath,
      // which we do not want to do.
      .filterNot { usage ->
        val identifier = if (coordinates is IncludedBuildCoordinates) {
          coordinates.resolvedProject.identifier
        } else {
          coordinates.identifier
        }

        val currentClasspaths = dependenciesToClasspaths.get(identifier)
        val isRuntimeUsage =
          usage.bucket == Bucket.API || usage.bucket == Bucket.IMPL || usage.bucket == Bucket.RUNTIME_ONLY

        // if it is a runtime usage and this dep isn't currently in the matching runtime classpath, don't add it there.
        isRuntimeUsage && !usage.runtimeMatches(currentClasspaths)
      }
      .mapTo(advice) { usage ->
        val preferredCoordinatesNotation =
          if (coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath) {
            coordinates.resolvedProject
          } else {
            coordinates
          }
        Advice.ofAdd(preferredCoordinatesNotation.withoutDefaultCapability(), mapper.toConfiguration(usage))
      }

    // Any remaining declarations should be removed
    declarations.asSequence()
      // Don't remove runtimeOnly or compileOnly declarations
      .filterNot { decl ->
        decl.bucket(configurationNames) == Bucket.COMPILE_ONLY || decl.bucket(configurationNames) == Bucket.RUNTIME_ONLY
      }
      .mapTo(advice) { declaration ->
        Advice.ofRemove(declarationCoordinates(declaration), declaration)
      }
  }

  /**
   * Simply advice by transforming matching pairs of add-advice and remove-advice into a single change-advice. Also
   * strips out confusing and verbose advice to change/add dependencies for the same product flavor.
   *
   * In addition, strip advice that would add redundant declarations in related source sets, or which would upgrade test
   * dependencies.
   */
  private fun simplify(advice: MutableSet<Advice>): Set<Advice> {
    val (add, remove, change) = advice.mutPartitionOf(
      { it.isAdd() || it.isCompileOnly() },
      { it.isRemove() || it.isRemoveCompileOnly() },
      { it.isAnyChange() },
    )

    // JVM, KMP
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
