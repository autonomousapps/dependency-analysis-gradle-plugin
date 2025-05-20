// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.transform

import com.autonomousapps.extension.DependenciesHandler
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.Coordinates.Companion.copy
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.declaration.internal.Bucket
import com.autonomousapps.model.declaration.internal.Declaration
import com.autonomousapps.model.internal.intermediates.Reason
import com.autonomousapps.model.internal.intermediates.Usage
import com.autonomousapps.model.source.SourceKind
import com.google.common.collect.SetMultimap
import org.gradle.api.attributes.Category

/**
 * Given [coordinates] and zero or more [declarations] for a given dependency, and the [usages][Usage] of that
 * dependency, emit a set of transforms, or advice, that a user can follow to produce simple and correct dependency
 * declarations in a build script.
 */
internal class StandardTransform(
  private val coordinates: Coordinates,
  private val declarations: Set<Declaration>,
  private val directDependencies: SetMultimap<String, SourceKind>,
  private val dependenciesToClasspaths: SetMultimap<String, String>,
  private val supportedSourceSets: Set<String>,
  private val buildPath: String,
  private val explicitSourceSets: Set<String> = emptySet(),
  private val isAndroidProject: Boolean,
  private val isKaptApplied: Boolean = false,
) : Usage.Transform {

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    var (mainUsages, testUsages, androidTestUsages, customJvmUsage) = usages.mutPartitionOf(
      { it.sourceKind.kind == SourceKind.MAIN_KIND },
      { it.sourceKind.kind == SourceKind.TEST_KIND },
      { it.sourceKind.kind == SourceKind.ANDROID_TEST_KIND },
      { it.sourceKind.kind == SourceKind.CUSTOM_JVM_KIND },
    )

    val hasCustomSourceSets = hasCustomSourceSets(usages)
    val (mainDeclarations, testDeclarations, androidTestDeclarations, customJvmDeclarations) =
      declarations.mutPartitionOf(
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.MAIN_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.TEST_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.ANDROID_TEST_KIND },
        { it.findSourceKind(hasCustomSourceSets)?.kind == SourceKind.CUSTOM_JVM_KIND },
      )

    /*
     * Main usages.
     */

    val singleVariant = mainUsages.size == 1
    val isMainVisibleDownstream = Bucket.isVisibleToTestSource(mainUsages, mainDeclarations)

    mainUsages = reduceUsages(mainUsages)
    computeAdvice(advice, mainUsages, mainDeclarations, singleVariant)

    /*
     * Test usages.
     */

    // If main usages are visible downstream, then we don't need a test declaration
    testUsages = if (isMainVisibleDownstream && !explicitFor("test")) {
      mutableSetOf()
    } else {
      reduceUsages(testUsages)
    }
    computeAdvice(advice, testUsages, testDeclarations, testUsages.size == 1)

    /*
     * Android test usages.
     */

    androidTestUsages = if (isMainVisibleDownstream && !explicitFor("androidTest")) {
      mutableSetOf()
    } else {
      reduceUsages(androidTestUsages)
    }
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
            && decl.bucket != Bucket.COMPILE_ONLY
            // Don't remove a declaration on runtimeOnly
            && decl.bucket != Bucket.RUNTIME_ONLY
          ) {
            advice += Advice.ofRemove(
              coordinates = declarationCoordinates(decl),
              declaration = decl
            )
          } else if (
            usage.bucket != Bucket.NONE
            // Don't change a match, it's correct!
            && !usage.bucket.matches(decl)
            // Don't change a declaration on compileOnly, compileOnlyApi, providedCompile
            && decl.bucket != Bucket.COMPILE_ONLY
            // Don't change a declaration on runtimeOnly
            && decl.bucket != Bucket.RUNTIME_ONLY
          ) {
            advice += Advice.ofChange(
              coordinates = declarationCoordinates(decl),
              fromConfiguration = decl.configurationName,
              toConfiguration = usage.toConfiguration()
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
          .find { usage.bucket.matches(it) }
          ?.let { theDecl ->
            // drain
            declarations.remove(theDecl)
            usageIter.remove()

            // Don't change a single-usage match, it's correct!
            if (!(singleVariant && usage.bucket.matches(theDecl))) {
              advice += Advice.ofChange(
                coordinates = declarationCoordinates(theDecl),
                fromConfiguration = theDecl.configurationName,
                toConfiguration = usage.toConfiguration()
              )
            }
          }
      }
    }

    // In the very common case that we have one single declaration and one single usage, we have special handling as a
    // matter of laziness. If the single declaration is both wrong _and_ on a variant, then we transform it to the
    // correct usage on that same variant. E.g., debugImplementation => debugRuntimeOnly. Without this block, the
    // algorithm would instead advise: debugImplementation => runtimeOnly.
    // See `should be debugRuntimeOnly` in StandardTransformTest.
    if (usages.size == 1 && declarations.size == 1) {
      val lastUsage = usages.first()
      if (lastUsage.bucket != Bucket.NONE) {
        val lastDeclaration = declarations.first()
        advice += Advice.ofChange(
          coordinates = declarationCoordinates(lastDeclaration),
          fromConfiguration = lastDeclaration.configurationName,
          toConfiguration = lastUsage.toConfiguration(
            forcedKind = lastDeclaration.findSourceKind(hasCustomSourceSets)
          )
        )

        // !!!early return!!!
        return
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
      // TODO(tsr): reconsider this. Craft a scenario where we'd remove one dep even though a transitive supplies a
      //  runtime capability and see what happens. We may want to add runtimeOnly declarations.
      .filterNot { usage -> usage.bucket == Bucket.RUNTIME_ONLY }
      // Don't add something that is only present on the compileClasspath as that will change the runtimeClasspath,
      // which we do not want to do.
      .filterNot { usage ->
        // coordinate with `ComputeAdviceTask::flattenDependencies`
        val identifier = if (coordinates is IncludedBuildCoordinates) {
          coordinates.resolvedProject.identifier
        } else {
          coordinates.identifier
        }

        val currentClasspaths = dependenciesToClasspaths.get(identifier)
        val isRuntimeUsage = usage.bucket == Bucket.API || usage.bucket == Bucket.IMPL

        // if it would be a runtime usage and this dep isn't currently in the matching runtime classpath, don't add it
        // there.
        isRuntimeUsage && !usage.runtimeMatches(currentClasspaths)
      }
      .mapTo(advice) { usage ->
        val preferredCoordinatesNotation =
          if (coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath) {
            coordinates.resolvedProject
          } else {
            coordinates
          }
        Advice.ofAdd(preferredCoordinatesNotation.withoutDefaultCapability(), usage.toConfiguration())
      }

    // Any remaining declarations should be removed
    declarations.asSequence()
      // Don't remove runtimeOnly or compileOnly declarations
      .filterNot { it.bucket == Bucket.COMPILE_ONLY || it.bucket == Bucket.RUNTIME_ONLY }
      .mapTo(advice) { declaration ->
        Advice.ofRemove(declarationCoordinates(declaration), declaration)
      }
  }

  private fun Declaration.findSourceKind(hasCustomSourceSets: Boolean): SourceKind? {
    return sourceSetKind(
      supportedSourceSets,
      isAndroidProject = isAndroidProject,
      hasCustomSourceSets = hasCustomSourceSets,
    )
  }

  /**
   * Returns true if [sourceSet] is in the set of [explicitSourceSets], or if [explicitSourceSets] is set for all source
   * sets.
   */
  private fun explicitFor(sourceSet: String?): Boolean {
    return sourceSet in explicitSourceSets
      || DependenciesHandler.isExplicitForAll(explicitSourceSets)
  }

  /** Use coordinates/variant of the original declaration when reporting remove/change as it is more precise. */
  private fun declarationCoordinates(decl: Declaration) = when {
    coordinates is IncludedBuildCoordinates && decl.identifier.startsWith(":") -> coordinates.resolvedProject
    else -> coordinates
  }.copy(decl.identifier, decl.gradleVariantIdentification)

  private fun hasCustomSourceSets(usages: Set<Usage>): Boolean {
    return usages.any { it.sourceKind.kind == SourceKind.CUSTOM_JVM_KIND }
  }

  /**
   * Simply advice by transforming matching pairs of add-advice and remove-advice into a single change-advice. In
   * addition, strip advice that would add redundant declarations in related source sets, or which would upgrade test
   * dependencies.
   */
  private fun simplify(advice: MutableSet<Advice>): Set<Advice> {
    val (add, remove) = advice.mutPartitionOf(
      { it.isAdd() || it.isCompileOnly() },
      { it.isRemove() || it.isRemoveCompileOnly() }
    )

    add.forEach { theAdd ->
      remove
        .find { it.coordinates == theAdd.coordinates }
        ?.let { theRemove ->
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

  /**
   * We don't want to be forced to redeclare dependencies in related source sets. Consider (pseudo-code):
   * ```
   * // build.gradle
   * sourceSets.functionalTest.extendsFrom sourceSets.test
   *
   * dependencies {
   *   testImplementation 'foo:bar:1.0'
   *   // functionalTestImplementation will also "inherit" the 'foo:bar:1.0' dependency.
   * }
   * ```
   *
   * nb: returning false means "keep this advice."
   */
  private fun isDeclaredInRelatedSourceSet(allAdvice: Set<Advice>, advice: Advice): Boolean {
    if (!advice.isAnyAdd()) return false

    val sourceSetName = DependencyScope.sourceSetName(advice.toConfiguration!!)

    // With explicit source sets, a source set may not be related to any other.
    if (explicitFor(sourceSetName)) return false

    val isTestRelated = sourceSetName?.let { DependencyScope.isTestRelated(it) } == true

    // Don't strip advice that improves correctness (e.g., declaring something on an "api-like" configuration).
    // Unless it's api-like on a test source set, which makes no sense.
    if (advice.isToApiLike() && !isTestRelated) return false

    // Instead of attempting a complex algorithm to get it "just right", if we see ANY downgrade advice, we bail out.
    // This particular function is already an optimization to support a scenario we arguably shouldn't, leading to
    // increasingly complex and brittle code. That is, it supports the special case that main deps are generally
    // visible to test. Perhaps we should just stop doing that.
    val anyDowngrade = allAdvice.any { it.isDowngrade() }
    if (anyDowngrade) return false

    val sourceSets = directDependencies[advice.coordinates.identifier].map { it.name }

    // There's "no point" in adding a new declaration when that dependency is already available as a direct dependency,
    // UNLESS we also happen to have some advice that might DOWNGRADE/REMOVE that declaration. For example, we might be
    // about to advise the user to remove an `implementation` dependency, in which case the advice to also add a
    // `testImplementation` dependency IS NOT redundant and SHOULD NOT be filtered out.
    return sourceSetName in sourceSets
  }

  /**
   * If we're adding an api-like declaration to a test-like configuration, instead suggest adding it to an
   * implementation-like configuration. Tests don't have APIs.
   */
  private fun downgradeTestDependencies(advice: Advice): Advice {
    if (!advice.isAnyAdd()) return advice
    if (!advice.isToApiLike()) return advice

    val sourceSetName = DependencyScope.sourceSetName(advice.toConfiguration!!) ?: return advice
    if (!DependencyScope.isTestRelated(sourceSetName)) return advice

    return advice.copy(toConfiguration = "${sourceSetName}Implementation")
  }

  /** e.g., "debug" + "implementation" -> "debugImplementation" */
  private fun Usage.toConfiguration(forcedKind: SourceKind? = null): String {
    check(bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

    fun processor() = if (isKaptApplied) "kapt" else "annotationProcessor"

    fun SourceKind.configurationNamePrefix(): String = when (kind) {
      SourceKind.MAIN_KIND -> name
      SourceKind.TEST_KIND -> SourceKind.TEST_NAME
      SourceKind.ANDROID_TEST_KIND -> SourceKind.ANDROID_TEST_NAME
      SourceKind.CUSTOM_JVM_KIND -> name
      else -> error("Unexpected kind: $kind")
    }

    fun SourceKind.configurationNameSuffix(): String = when (kind) {
      SourceKind.MAIN_KIND -> name.replaceFirstChar(Char::uppercase)
      SourceKind.TEST_KIND -> "Test"
      SourceKind.ANDROID_TEST_KIND -> "AndroidTest"
      SourceKind.CUSTOM_JVM_KIND -> name.replaceFirstChar(Char::uppercase)
      else -> error("Unexpected kind: $kind")
    }

    val theSourceKind = forcedKind ?: sourceKind

    if (bucket == Bucket.ANNOTATION_PROCESSOR) {
      val original = processor()
      return if (theSourceKind.name == SourceKind.MAIN_NAME) {
        // "main" + "annotationProcessor" -> "annotationProcessor"
        // "main" + "kapt" -> "kapt"
        when (original) {
          "annotationProcessor" -> "annotationProcessor"
          "kapt" -> "kapt"
          else -> throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      } else {
        // "debug" + "annotationProcessor" -> "debugAnnotationProcessor"
        // "test" + "kapt" -> "kaptTest"
        when (original) {
          "annotationProcessor" -> "${theSourceKind.configurationNamePrefix()}AnnotationProcessor"
          "kapt" -> "kapt${theSourceKind.configurationNameSuffix()}"
          else -> throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      }
    }

    // not an annotation processor
    return if (theSourceKind.name == SourceKind.MAIN_NAME && theSourceKind.kind == SourceKind.MAIN_KIND) {
      // "main" + "api" -> "api"
      bucket.value
    } else {
      // "debug" + "implementation" -> "debugImplementation"
      // "test" + "implementation" -> "testImplementation"
      "${theSourceKind.configurationNamePrefix()}${bucket.value.capitalizeSafely()}"
    }
  }
}

private fun Set<Declaration>.forCoordinates(coordinates: Coordinates): Set<Declaration> {
  return asSequence()
    .filter { declaration ->
      declaration.identifier == coordinates.identifier
        // In the special case of IncludedBuildCoordinates, the declaration might be a 'project(...)' dependency
        // if subprojects inside an included build depend on each other.
        || (coordinates is IncludedBuildCoordinates) && declaration.identifier == coordinates.resolvedProject.identifier
    }
    .filter { it.isJarDependency() && it.gradleVariantIdentification.variantMatches(coordinates) }
    .toSet()
}

private fun isSingleBucketForSingleVariant(usages: Set<Usage>): Boolean {
  return if (usages.size == 1) {
    true
  } else {
    usages.mapToSet { it.bucket }.size == 1 && usages.mapToSet { it.sourceKind.base() }.size == 1
  }
}

private fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }

/**
 * Does the dependency point to one (or multiple) Jars, or is it just Metadata (i.e. a platform)
 * that we always want to keep?
 */
private fun Declaration.isJarDependency() =
  gradleVariantIdentification.attributes[Category.CATEGORY_ATTRIBUTE.name].let {
    it != Category.REGULAR_PLATFORM && it != Category.ENFORCED_PLATFORM
  }
