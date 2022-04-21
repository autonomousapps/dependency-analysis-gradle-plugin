package com.autonomousapps.transform

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Declaration
import com.autonomousapps.model.intermediates.Usage
import org.gradle.api.tasks.SourceSet

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

    val usagesBySourceSet = usages.groupBy { it.variant.sourceSetName }
    val declarationsBySourceSet = declarations.groupBy { it.variant.sourceSetName }

    // TODO only an assumption. Depends on the concrete setup of inheritance between configurations.
    val isMainVisibleDownstream =
      usagesBySourceSet.getOrDefault(SourceSet.MAIN_SOURCE_SET_NAME, emptyList()).reallyAll { usage ->
        Bucket.VISIBLE_TO_TEST_SOURCE.any { b -> b == usage.bucket }
      }

    usagesBySourceSet.forEach { (sourceSetName, sourceSetUsages) ->
      val singleVariant = sourceSetUsages.size == 1
      // If main usages are visible downstream, then we don't need a test declaration
      val reducedUsages = if (isMainVisibleDownstream && sourceSetName == SourceSet.TEST_SOURCE_SET_NAME)
        mutableSetOf() else reduceUsages(sourceSetUsages.toMutableSet())
      computeAdvice(
        advice, reducedUsages,
        declarationsBySourceSet.getOrDefault(sourceSetName, emptyList()).toMutableSet(), singleVariant
      )
    }

    return simplify(advice)
  }

  /** Reduce usages to fewest possible (1+). */
  private fun reduceUsages(usages: MutableSet<Usage>): MutableSet<Usage> {
    if (usages.isEmpty()) return usages

    val sourceSets = usages.mapToSet { it.variant.sourceSetName }
    check(sourceSets.size == 1) { "Expected a single ${SourceSet::class.java.simpleName}. Got: $sourceSets" }

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
            toConfiguration = usage.toConfiguration()
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
                toConfiguration = usage.toConfiguration()
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
  private fun Usage.toConfiguration(): String {
    check(bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

    fun processor(): String {
      return if (isKaptApplied) "kapt" else "annotationProcessor"
    }

    if (bucket == Bucket.ANNOTATION_PROCESSOR) {
      val original = processor()
      return if ("annotationProcessor" in original) {
        // "main" + "annotationProcessor" -> "annotationProcessor"
        // "debug" + "annotationProcessor" -> "debugAnnotationProcessor"
        variant.variantSpecificBucketName("annotationProcessor")
      } else if ("kapt" in original) {
        // "test" + "kapt" -> "kaptTest"
        // "main" + "kapt" -> "kapt"
        variant.variantSpecificBucketName("kapt")
      } else {
        throw IllegalArgumentException("Unknown annotation processor: $original")
      }
    }

    return variant.variantSpecificBucketName(bucket.value)
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

private fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }
