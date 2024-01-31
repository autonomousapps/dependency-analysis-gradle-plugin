// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.transform

import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.Coordinates.Companion.copy
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.model.intermediates.Reason
import com.autonomousapps.model.intermediates.Usage
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
  private val nonTransitiveDependencies: SetMultimap<String, Variant>,
  private val supportedSourceSets: Set<String>,
  private val buildPath: String,
  private val isKaptApplied: Boolean = false,
) : Usage.Transform {

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    var (mainUsages, testUsages, androidTestUsages, customJvmUsage) = usages.mutPartitionOf(
      { it.variant.kind == SourceSetKind.MAIN },
      { it.variant.kind == SourceSetKind.TEST },
      { it.variant.kind == SourceSetKind.ANDROID_TEST },
      { it.variant.kind == SourceSetKind.CUSTOM_JVM }
    )

    val hasCustomSourceSets = hasCustomSourceSets(usages)
    val (mainDeclarations, testDeclarations, androidTestDeclarations, customJvmDeclarations) =
      declarations.mutPartitionOf(
        { it.variant(supportedSourceSets, hasCustomSourceSets)?.kind == SourceSetKind.MAIN },
        { it.variant(supportedSourceSets, hasCustomSourceSets)?.kind == SourceSetKind.TEST },
        { it.variant(supportedSourceSets, hasCustomSourceSets)?.kind == SourceSetKind.ANDROID_TEST },
        { it.variant(supportedSourceSets, hasCustomSourceSets)?.kind == SourceSetKind.CUSTOM_JVM }
      )

    /*
     * Main usages.
     */

    val singleVariant = mainUsages.size == 1
    val isMainVisibleDownstream = mainUsages.reallyAll { usage ->
      Bucket.VISIBLE_TO_TEST_SOURCE.any { it == usage.bucket }
    }
    mainUsages = reduceUsages(mainUsages)
    computeAdvice(advice, mainUsages, mainDeclarations, singleVariant)

    /*
     * Test usages.
     */

    // If main usages are visible downstream, then we don't need a test declaration
    testUsages = if (isMainVisibleDownstream) mutableSetOf() else reduceUsages(testUsages)
    computeAdvice(advice, testUsages, testDeclarations, testUsages.size == 1)

    /*
     * Android test usages.
     */

    androidTestUsages = if (isMainVisibleDownstream) mutableSetOf() else reduceUsages(androidTestUsages)
    computeAdvice(advice, androidTestUsages, androidTestDeclarations, androidTestUsages.size == 1)

    /*
     * Custom JVM source sets like 'testFixtures', 'integrationTest' or other custom source sets and feature variants
     */

    customJvmUsage = reduceUsages(customJvmUsage)
    computeAdvice(advice, customJvmUsage, customJvmDeclarations, customJvmUsage.size == 1)

    return simplify(advice)
  }

  /** Reduce usages to fewest possible (1+). */
  private fun reduceUsages(usages: MutableSet<Usage>): MutableSet<Usage> {
    if (usages.isEmpty()) return usages

    val kinds = usages.mapToSet { it.variant.kind }
    check(kinds.size == 1) { "Expected a single ${SourceSetKind::class.java.simpleName}. Got: $kinds" }

    // This could be a JVM module or an Android module only analyzing a singe variant. For the latter, we need to
    // transform it into a "main" variant.
    return if (usages.size == 1) {
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        variant = usage.variant.base(),
        bucket = usage.bucket,
        reasons = usage.reasons
      ).intoMutableSet()
    } else if (!isSingleBucketForSingleVariant(usages)) {
      // More than one usage _and_ multiple buckets: in a variant situation (Android), there are no "main" usages, by
      // definition. Everything is debugImplementation, releaseApi, etc. If each variant has a different usage, we
      // respect that. In JVM, each variant is distinct (feature variant).
      usages
    } else {
      // More than one usage, but all in the same bucket with the same variant. We reduce the usages to a single usage.
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        variant = usage.variant.base(),
        bucket = usage.bucket,
        reasons = usages.flatMapToSet { it.reasons }
      ).intoMutableSet()
    }
  }

  /** Turn usage information into actionable advice. */
  private fun computeAdvice(
    advice: MutableSet<Advice>,
    usages: MutableSet<Usage>,
    declarations: MutableSet<Declaration>,
    singleVariant: Boolean,
  ) {
    val usageIter = usages.iterator()
    val hasCustomSourceSets = hasCustomSourceSets(usages)
    while (usageIter.hasNext()) {
      val usage = usageIter.next()
      val declarationsForVariant = declarations.filterToSet { declaration ->
        declaration.variant(supportedSourceSets, hasCustomSourceSets) == usage.variant
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
      } else {
        // No exact match, so look for a declaration on the same bucket (e.g., usage is 'api' and declaration is
        // 'debugApi').
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
            forceVariant = lastDeclaration.variant(supportedSourceSets, hasCustomSourceSets)
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
      .filterNot { it.bucket == Bucket.RUNTIME_ONLY || it.bucket == Bucket.COMPILE_ONLY }
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

  /** Use coordinates/variant of the original declaration when reporting remove/change as it is more precise. */
  private fun declarationCoordinates(decl: Declaration) = when {
    coordinates is IncludedBuildCoordinates && decl.identifier.startsWith(":") -> coordinates.resolvedProject
    else -> coordinates
  }.copy(decl.identifier, decl.gradleVariantIdentification)

  private fun hasCustomSourceSets(usages: Set<Usage>) =
    usages.any { it.variant.kind == SourceSetKind.CUSTOM_JVM }

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
    advice.removeIf(::isDeclaredInRelatedSourceSet)

    return advice
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
   */
  private fun isDeclaredInRelatedSourceSet(advice: Advice): Boolean {
    if (!advice.isAnyAdd()) return false

    val sourceSetName = DependencyScope.sourceSetName(advice.toConfiguration!!)
    val sourceSets = nonTransitiveDependencies[advice.coordinates.identifier].map { it.variant }

    return sourceSetName in sourceSets
  }

  /** e.g., "debug" + "implementation" -> "debugImplementation" */
  private fun Usage.toConfiguration(forceVariant: Variant? = null): String {
    check(bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

    fun processor() = if (isKaptApplied) "kapt" else "annotationProcessor"

    fun Variant.configurationNamePrefix(): String = when (kind) {
      SourceSetKind.MAIN -> variant
      SourceSetKind.TEST -> "test"
      SourceSetKind.ANDROID_TEST -> "androidTest"
      SourceSetKind.CUSTOM_JVM -> variant
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun Variant.configurationNameSuffix(): String = when (kind) {
      SourceSetKind.MAIN -> variant.replaceFirstChar(Char::uppercase)
      SourceSetKind.TEST -> "Test"
      SourceSetKind.ANDROID_TEST -> "AndroidTest"
      SourceSetKind.CUSTOM_JVM -> variant.replaceFirstChar(Char::uppercase)
    }

    val theVariant = forceVariant ?: variant

    if (bucket == Bucket.ANNOTATION_PROCESSOR) {
      val original = processor()
      return if (theVariant.variant == Variant.MAIN_NAME) {
        // "main" + "annotationProcessor" -> "annotationProcessor"
        // "main" + "kapt" -> "kapt"
        if ("annotationProcessor" in original) {
          "annotationProcessor"
        } else if ("kapt" in original) {
          "kapt"
        } else {
          throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      } else {
        // "debug" + "annotationProcessor" -> "debugAnnotationProcessor"
        // "test" + "kapt" -> "kaptTest"
        if ("annotationProcessor" in original) {
          "${theVariant.configurationNamePrefix()}AnnotationProcessor"
        } else if ("kapt" in original) {
          "kapt${theVariant.configurationNameSuffix()}"
        } else {
          throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      }
    }

    return if (theVariant.variant == Variant.MAIN_NAME && theVariant.kind == SourceSetKind.MAIN) {
      // "main" + "api" -> "api"
      bucket.value
    } else {
      // "debug" + "implementation" -> "debugImplementation"
      // "test" + "implementation" -> "testImplementation"
      "${theVariant.configurationNamePrefix()}${bucket.value.capitalizeSafely()}"
    }
  }
}

private fun Set<Declaration>.forCoordinates(coordinates: Coordinates): Set<Declaration> {
  return asSequence()
    .filter {
      it.identifier == coordinates.identifier ||
        // In the special case of IncludedBuildCoordinates, the declaration might be a 'project(...)' dependency
        // if subprojects inside an included build depend on each other.
        (coordinates is IncludedBuildCoordinates) && it.identifier == coordinates.resolvedProject.identifier
    }
    .filter { it.isJarDependency() && it.gradleVariantIdentification.variantMatches(coordinates) }
    .toSet()
}

private fun isSingleBucketForSingleVariant(usages: Set<Usage>): Boolean {
  return if (usages.size == 1) true
  else usages.mapToSet { it.bucket }.size == 1 && usages.mapToSet { it.variant.base() }.size == 1
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
