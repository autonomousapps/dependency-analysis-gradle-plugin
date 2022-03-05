package com.autonomousapps.transform

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Bucket
import com.autonomousapps.model.intermediates.Declaration
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.model.intermediates.Variant

/**
 * Given [coordinates] and zero or more [declarations] for a given dependency, and the [usages][Usage] of that
 * dependency, emit a set of transforms, or advice, that a user can follow to produce simple and correct dependency
 * declarations in a build script.
 */
internal class StandardTransform(
  private val coordinates: Coordinates,
  private val declarations: Set<Declaration>,
  private val isKaptApplied: Boolean = false
) : Usage.Transform {

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    var (mainUsages, testUsages) = usages.mutPartitionOf(
      { it.variant.kind == SourceSetKind.MAIN },
      { it.variant.kind == SourceSetKind.TEST }
    )
    val (mainDeclarations, testDeclarations) = declarations.mutPartitionOf(
      { it.variant.kind == SourceSetKind.MAIN },
      { it.variant.kind == SourceSetKind.TEST }
    )

    /*
     * Main usages.
     */

    val singleVariant = mainUsages.size == 1
    val isMainVisibleDownstream = mainUsages.reallyAll { usage ->
      Bucket.VISIBLE_DOWNSTREAM.any { usage.bucket == it }
    }
    mainUsages = reduceUsages(mainUsages)
    computeAdvice(advice, mainUsages, mainDeclarations, singleVariant)

    /*
     * Test usages.
     */

    // If main usages are visible downstream, then we don't need a test declaration
    testUsages = if (isMainVisibleDownstream) mutableSetOf() else reduceUsages(testUsages)
    computeAdvice(advice, testUsages, testDeclarations, testUsages.size == 1)

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
    } else if (!isSingleBucket(usages)) {
      // More than one usage _and_ multiple buckets: in a variant situation (Android), there are no "main" usages, by
      // definition. Everything is debugImplementation, releaseApi, etc. If each variant has a different usage, we
      // respect that.
      usages
    } else {
      // More than one usage, but all in the same bucket. So, we reduce the usages to a single usage.
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
    singleVariant: Boolean
  ) {
    val usageIter = usages.iterator()
    while (usageIter.hasNext()) {
      val usage = usageIter.next()
      val decl = declarations.find { it.variant == usage.variant }

      // We have a declaration on the same variant as the usage. Remove or change it, if necessary.
      if (decl != null) {
        // drain
        declarations.remove(decl)
        usageIter.remove()

        if (
          usage.bucket == Bucket.NONE
          // Don't remove a declaration on compileOnly
          && decl.bucket != Bucket.COMPILE_ONLY
          // Don't remove a declaration on runtimeOnly
          && decl.bucket != Bucket.RUNTIME_ONLY
        ) {
          advice += Advice.ofRemove(
            coordinates = coordinates,
            declaration = decl
          )
        } else if (
        // Don't change a match, it's correct!
          !usage.bucket.matches(decl)
          // Don't change a declaration on compileOnly
          && decl.bucket != Bucket.COMPILE_ONLY
          // Don't change a declaration on runtimeOnly
          && decl.bucket != Bucket.RUNTIME_ONLY
          // Don't change any declaration to runtimeOnly
          && usage.bucket != Bucket.RUNTIME_ONLY
        ) {
          advice += Advice.ofChange(
            coordinates = coordinates,
            fromConfiguration = decl.configurationName,
            toConfiguration = usage.toConfiguration(decl)
          )
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
            if ((!(singleVariant && usage.bucket.matches(theDecl)))) {
              advice += Advice.ofChange(
                coordinates = coordinates,
                fromConfiguration = theDecl.configurationName,
                toConfiguration = usage.toConfiguration(theDecl)
              )
            }
          }
      }
    }

    // Any remaining usages should be added
    usages.asSequence()
      // Don't add unused usages!
      .filterUsed()
      // Don't add runtimeOnly or compileOnly declarations
      .filterNot { it.bucket == Bucket.RUNTIME_ONLY || it.bucket == Bucket.COMPILE_ONLY }
      .mapTo(advice) { usage ->
        Advice.ofAdd(coordinates, usage.toConfiguration())
      }

    // Any remaining declarations should be removed
    declarations.asSequence()
      // Don't remove runtimeOnly or compileOnly declarations
      .filterNot { it.bucket == Bucket.COMPILE_ONLY || it.bucket == Bucket.RUNTIME_ONLY }
      .mapTo(advice) { decl ->
        Advice.ofRemove(coordinates, decl)
      }
  }

  /** Simply advice by transforming matching pairs of add-advice and remove-advice into a single change-advice. */
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

    return advice
  }

  /** e.g., "debug" + "implementation" -> "debugImplementation" */
  private fun Usage.toConfiguration(originalDeclaration: Declaration? = null): String {
    check(bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

    fun processor(): String {
      return if (isKaptApplied) "kapt" else "annotationProcessor"
    }

    if (bucket == Bucket.ANNOTATION_PROCESSOR) {
      return if (variant.variant == Variant.VARIANT_NAME_MAIN) {
        // "main" + "annotationProcessor" -> "annotationProcessor"
        // "main" + "kapt" -> "kapt"
        val original = processor()
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
        val original = processor()
        if ("annotationProcessor" in original) {
          "${configurationNamePrefix()}AnnotationProcessor"
        } else if ("kapt" in original) {
          "kapt${configurationNameSuffix()}"
        } else {
          throw IllegalArgumentException("Unknown annotation processor: $original")
        }
      }
    }

    return if (variant.variant == Variant.VARIANT_NAME_MAIN) {
      // "main" + "api" -> "api"
      bucket.value
    } else {
      // "debug" + "implementation" -> "debugImplementation"
      // "test" + "implementation" -> "testImplementation"
      "${configurationNamePrefix()}${bucket.value.capitalizeSafely()}"
    }
  }
}

private fun Set<Declaration>.forCoordinates(coordinates: Coordinates): Set<Declaration> {
  return asSequence()
    .filter { it.identifier == coordinates.identifier }
    // For now, we ignore any special dependencies like test fixtures or platforms
    .filter { it.attributes.isEmpty() }
    .toSet()
}

private fun isSingleBucket(usages: Set<Usage>): Boolean {
  return if (usages.size == 1) true
  else usages.mapToSet { it.bucket }.size == 1
}

private fun Usage.configurationNamePrefix(): String = when (variant.kind) {
  SourceSetKind.MAIN -> variant.variant
  SourceSetKind.TEST -> "test"
}

@OptIn(ExperimentalStdlibApi::class)
private fun Usage.configurationNameSuffix(): String = when (variant.kind) {
  SourceSetKind.MAIN -> variant.variant.replaceFirstChar(Char::uppercase)
  SourceSetKind.TEST -> "Test"
}

private fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }
