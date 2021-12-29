package com.autonomousapps.transform

import com.autonomousapps.internal.configuration.Configurations
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.model.intermediates.Bucket
import com.autonomousapps.model.intermediates.Declaration
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.model.intermediates.Variant

internal class StandardTransform(
  private val coordinates: Coordinates,
  private val declarations: Set<Declaration>
) : Usage.Transform {

  fun reduce2(usages: Set<Usage>): Set<Advice> {
    val advice = mutableSetOf<Advice>()

    val declarations = declarations.forCoordinates(coordinates)

    var (mainUsages, testUsages) = usages.mutPartitionOf(
      { it.kind == SourceSetKind.MAIN },
      { it.kind == SourceSetKind.TEST }
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

  /**
   * Reduce usages to fewest possible (1+).
   */
  private fun reduceUsages(usages: MutableSet<Usage>): MutableSet<Usage> {
    if (usages.isEmpty()) return usages

    val kinds = usages.mapToSet { it.kind }
    check(kinds.size == 1) { "Expected a single ${SourceSetKind::class.java.simpleName}. Got: $kinds" }

    // This could be a JVM module or an Android module only analyzing a singe variant. For the latter, we need to
    // transform it into a "main" variant.
    return if (usages.size == 1) {
      val usage = usages.first()
      Usage(
        buildType = null,
        flavor = null,
        variant = usage.kind.variantName,
        kind = usage.kind,
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
        variant = usage.kind.variantName,
        kind = usage.kind,
        bucket = usage.bucket,
        reasons = usages.flatMapToSet { it.reasons }
      ).intoMutableSet()
    }
  }

  /**
   * Turn usage information into actionable advice.
   */
  private fun computeAdvice(
    advice: MutableSet<Advice>,
    usages: MutableSet<Usage>,
    declarations: MutableSet<Declaration>,
    singleVariant: Boolean
  ) {
    val usageIter = usages.iterator()
    while (usageIter.hasNext()) {
      val usage = usageIter.next()
      val decl = declarations.find { it.variant == usage.theVariant }

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

            if (
            // Don't change a single-usage match, it's correct!
              (!(singleVariant && usage.bucket.matches(theDecl)))
            ) {
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

  /**
   * Simply advice by transforming matching pairs of add-advice and remove-advice into a single change-advice.
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

    return advice
  }

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return reduce2(usages)
    // val locations = declarations.forCoordinates(coordinates)
    //
    // check(usages.isNotEmpty())
    //
    // return if (locations.isEmpty()) {
    //   NoLocationTransform(coordinates).reduce(usages)
    // } else if (locations.size == 1) {
    //   val theLocation = locations.first()
    //   SingleLocationTransform(coordinates, theLocation).reduce(usages)
    // } else {
    //   MultiLocationTransform(coordinates, locations).reduce(usages)
    // }
  }
}

/**
 * These are undeclared dependencies that _might_ need to be added.
 * 1. They might also be unused -> do nothing.
 * 2. They might be used for compileOnly or runtimeOnly -> do nothing.
 */
private class NoLocationTransform(private val coordinates: Coordinates) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (isSingleBucket(usages)) {
      SingleBucket.reduce(coordinates, usages.first())
    } else {
      MultiLocationTransform(coordinates, emptySet()).reduce(usages)
      // MultiBucket.reduce(coordinates, usages)
    }
  }

  private object SingleBucket {
    fun reduce(coordinates: Coordinates, usage: Usage): Set<Advice> {
      return when (val bucket = usage.bucket) {
        Bucket.NONE -> emptySet()
        Bucket.RUNTIME_ONLY, Bucket.COMPILE_ONLY -> emptySet()
        else -> Advice.ofAdd(coordinates, bucket.value).intoSet()
      }
    }
  }

  private object MultiBucket {
    fun reduce(coordinates: Coordinates, usages: Set<Usage>): Set<Advice> {
      return usages.asSequence()
        // In a multi-bucket, zero-location scenario, if any of the buckets is NONE, ignore it, because it would be
        // nonsensical to advise people to add a dependency that they're not using.
        .filterUsed()
        .mapToConfiguration()
        .map { toConfiguration ->
          Advice.ofAdd(
            coordinates = coordinates,
            toConfiguration = toConfiguration
          )
        }
        .toSortedSet()
    }
  }
}

private class SingleLocationTransform(
  private val coordinates: Coordinates,
  private val declaration: Declaration
) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (isSingleBucket(usages)) {
      SingleBucket.reduce(coordinates, usages.first(), declaration)
    } else {
      MultiLocationTransform(coordinates, setOf(declaration)).reduce(usages)
      // MultiBucket.reduce(coordinates, usages, location)
    }
  }

  private object SingleBucket {
    fun reduce(coordinates: Coordinates, usage: Usage, declaration: Declaration): Set<Advice> {
      return if (usage.bucket.matches(declaration) && usage.kind == declaration.variant.kind) {
        emptySet()
      } else if (declaration.bucket == Bucket.COMPILE_ONLY) {
        // TODO: for compatibility with existing functional tests, don't suggest removing a dep that is declared
        //  compileOnly, but I'm not convinced this is what we want long-term.
        emptySet()
      } else if (usage.bucket == Bucket.RUNTIME_ONLY) {
        // TODO: for compatibility with existing functional tests, don't suggest changing a dep to runtimeOnly
        //  but I'm not convinced this is what we want long-term.
        emptySet()
      } else if (usage.bucket == Bucket.NONE) {
        Advice.ofRemove(
          coordinates = coordinates,
          fromConfiguration = declaration.configurationName
        ).intoSet()
      } else {
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = declaration.configurationName,
          toConfiguration = usage.bucket.value
        ).intoSet()
      }
    }
  }

  private object MultiBucket {
    fun reduce(coordinates: Coordinates, usages: Set<Usage>, declaration: Declaration): Set<Advice> {
      return usages.asSequence()
        // In a multi-bucket, single-location scenario, if any of the buckets is NONE, ignore it, because really what
        // we're doing is _changing_ the declaration to something variant-specific.
        .filterUsed()
        .mapToConfiguration()
        .map { toConfiguration ->
          Advice.ofChange(
            coordinates = coordinates,
            fromConfiguration = declaration.configurationName,
            toConfiguration = toConfiguration
          )
        }
        .toSortedSet()
    }
  }
}

private class MultiLocationTransform(
  private val coordinates: Coordinates,
  private val declarations: Set<Declaration>
) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (isSingleBucket(usages)) {
      SingleBucket.reduce(coordinates, usages.first(), declarations)
    } else {
      MultiBucket.reduce(coordinates, usages, declarations)
    }
  }

  private object SingleBucket {
    fun reduce(coordinates: Coordinates, usage: Usage, declarations: Set<Declaration>): Set<Advice> {
      val theBucket = usage.bucket
      val anyMain = declarations.any { it.variant == Variant.MAIN }

      return declarations.asSequence()
        .mapNotNull { declaration ->
          if (theBucket == Bucket.NONE) {
            Advice.ofRemove(
              coordinates = coordinates,
              fromConfiguration = declaration.configurationName
            )
          } else if (declaration.variant == Variant.MAIN) {
            // Don't change the main declaration
            null
          } else if (anyMain) {
            // If any location is the "main" (w/o variant) location, then we just remove variant-specific locations
            Advice.ofRemove(
              coordinates = coordinates,
              fromConfiguration = declaration.configurationName
            )
          } else {
            Advice.ofChange(
              coordinates = coordinates,
              fromConfiguration = declaration.configurationName,
              toConfiguration = theBucket.value
            )
          }
        }
        .toSortedSet()
    }
  }

  private object MultiBucket {
    fun reduce(coordinates: Coordinates, usages: Set<Usage>, declarations: Set<Declaration>): Set<Advice> {
      val advice = sortedSetOf<Advice>()

      val (mainUsages, testUsages) = usages.mutPartitionOf(
        { it.kind == SourceSetKind.MAIN },
        { it.kind == SourceSetKind.TEST }
      )

      val (mainLocations, testLocations) = declarations.mutPartitionOf(
        { it.variant.kind == SourceSetKind.MAIN },
        { it.variant.kind == SourceSetKind.TEST }
      )

      /*
       * Handle main source set usages and declarations.
       */

      val mainAdvice = mutableSetOf<Advice>()
      val mainUsage = mainUsages.firstOrNull()

      // "Remove" all current declarations
      mainLocations.mapTo(mainAdvice) { Advice.ofRemove(coordinates, it) }

      // "Add" all relevant usages
      var singleBucket = false
      if (isSingleBucket(mainUsages) && mainUsage!!.bucket != Bucket.NONE) {
        singleBucket = true
        // For a single usage, add to main declaration (e.g., "api", not "debugApi")
        mainAdvice += Advice.ofAdd(coordinates, mainUsage.bucket.value)
      } else {
        // For multiple usages, add to variant-specific configs (e.g., "debugApi", not "api")
        mainUsages.asSequence()
          .filterUsed()
          .mapTo(mainAdvice) { Advice.ofAdd(coordinates, it.toConfiguration()) }
      }

      // Reduce removes + adds => changes (where possible)
      reduceAdvice(mainAdvice)

      /*
       * Handle test source set usages and declarations.
       */

      val testAdvice = mutableSetOf<Advice>()

      // "Remove" all current declarations
      testLocations.mapTo(testAdvice) { Advice.ofRemove(coordinates, it) }

      if (!singleBucket) {
        // Either there will be multiple main declarations or this dep isn't used at all by main: declare on test
        testUsages.asSequence()
          .filterUsed()
          .mapTo(testAdvice) { Advice.ofAdd(coordinates, it.toConfiguration()) }
      }

      // Reduce removes + adds => changes (where possible)
      reduceAdvice(testAdvice)

      // Add to final advice (sorted set)
      advice += mainAdvice
      advice += testAdvice

      // Reduce removes + adds => changes (where possible)
      reduceAdvice(advice)

      return advice
    }

    private fun reduceAdvice(advice: MutableSet<Advice>) {
      val (removeAdvice, addAdvice) = advice.mutPartitionOf(
        { it.isRemove() || it.isProcessor() },
        { it.isAdd() || it.isCompileOnly() }
      )

      var iter = removeAdvice.iterator()
      while (iter.hasNext()) {
        val remove = iter.next()
        val removeVariant = Configurations.variantFrom(remove.fromConfiguration!!)
        val add = addAdvice.find { Configurations.variantFrom(it.toConfiguration!!) == removeVariant }
        if (add != null) {
          // drain
          iter.remove()
          addAdvice -= add

          advice -= remove
          advice -= add

          if (isValidChange(remove, add)) {
            advice += Advice.ofChange(
              coordinates = remove.coordinates,
              fromConfiguration = remove.fromConfiguration,
              toConfiguration = add.toConfiguration!!
            )
          }
        }
      }

      // if any are left, use buckets for matching
      iter = removeAdvice.iterator()
      while (iter.hasNext()) {
        val remove = iter.next()
        val removeBucket = Bucket.of(remove.fromConfiguration!!)
        val add = addAdvice.find { Bucket.of(it.toConfiguration!!) == removeBucket }
        if (add != null) {
          // drain
          addAdvice -= add

          advice -= remove
          advice -= add

          if (isValidChange(remove, add)) {
            advice += Advice.ofChange(
              coordinates = remove.coordinates,
              fromConfiguration = remove.fromConfiguration,
              toConfiguration = add.toConfiguration!!
            )
          }
        }
      }

      // if any are left, be more promiscuous in matching
      removeAdvice.forEach { remove ->
        val add = addAdvice.firstOrNull()
        if (add != null) {
          // drain
          addAdvice -= add

          advice -= remove
          advice -= add

          if (isValidChange(remove, add)) {
            advice += Advice.ofChange(
              coordinates = remove.coordinates,
              fromConfiguration = remove.fromConfiguration!!,
              toConfiguration = add.toConfiguration!!
            )
          }
        }
      }
    }

    private fun isValidChange(remove: Advice, add: Advice): Boolean {
      if (remove.isRemoveCompileOnly()) return false
      if (add.isCompileOnly()) return false
      if (remove.isRemoveRuntimeOnly()) return false
      if (add.isRuntimeOnly()) return false

      // this would be silly
      return remove.fromConfiguration != add.toConfiguration
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

/** e.g., "debug" + "implementation" -> "debugImplementation" */
private fun Sequence<Usage>.mapToConfiguration() = map { it.toConfiguration() }

/** e.g., "debug" + "implementation" -> "debugImplementation" */
private fun Usage.toConfiguration(): String {
  check(bucket != Bucket.NONE) { "You cannot 'declare' an unused dependency" }

  // TODO V2: I don't think this works for test + variant yet
  return if (variant == Variant.VARIANT_NAME_MAIN) {
    // "main" + "api" -> "api"
    bucket.value
  } else {
    // "debug" + "implementation" -> "debugImplementation"
    // "test" + "implementation" -> "testImplementation"
    "${configurationNamePrefix()}${bucket.value.capitalizeSafely()}"
  }
}

private fun Usage.configurationNamePrefix() = when (kind) {
  SourceSetKind.MAIN -> variant
  SourceSetKind.TEST -> "test"
}

private fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }
