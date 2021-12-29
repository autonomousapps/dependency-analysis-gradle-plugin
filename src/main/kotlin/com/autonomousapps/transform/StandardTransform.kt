package com.autonomousapps.transform

import com.autonomousapps.internal.utils.capitalizeSafely
import com.autonomousapps.internal.utils.intoSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.intermediates.Bucket
import com.autonomousapps.model.intermediates.Location
import com.autonomousapps.model.intermediates.Usage
import com.autonomousapps.model.intermediates.Variant

internal class StandardTransform(
  private val coordinates: Coordinates,
  private val locations: Set<Location>
) : Usage.Transform {

  override fun reduce(usages: Set<Usage>): Set<Advice> {
    val locations = locationsFor(coordinates)

    check(usages.isNotEmpty())

    return if (locations.isEmpty()) {
      NoLocationTransform(coordinates).reduce(usages)
    } else if (locations.size == 1) {
      val theLocation = locations.first()
      SingleLocationTransform(coordinates, theLocation).reduce(usages)
    } else {
      MultiLocationTransform(coordinates, locations).reduce(usages)
    }
  }

  private fun locationsFor(coordinates: Coordinates): Set<Location> {
    return locations.asSequence()
      .filter { it.identifier == coordinates.identifier }
      // For now, we ignore any special dependencies like test fixtures or platforms
      .filter { it.attributes.isEmpty() }
      .toSet()
  }
}

/**
 * These are undeclared dependencies that _might_ need to be added.
 * 1. They might also be unused -> do nothing.
 * 2. They might be used for compileOnly or runtimeOnly -> do nothing.
 */
private class NoLocationTransform(private val coordinates: Coordinates) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (usages.size == 1 || countBuckets(usages) == 1) {
      SingleBucket.reduce(coordinates, usages.first())
    } else {
      MultiBucket.reduce(coordinates, usages)
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
  private val location: Location
) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (usages.size == 1 || countBuckets(usages) == 1) {
      SingleBucket.reduce(coordinates, usages.first(), location)
    } else {
      MultiBucket.reduce(coordinates, usages, location)
    }
  }

  private object SingleBucket {
    fun reduce(coordinates: Coordinates, usage: Usage, location: Location): Set<Advice> {
      val theBucket = usage.bucket
      return if (theBucket.matches(location)) {
        emptySet()
      } else if (location.bucket == Bucket.COMPILE_ONLY) {
        // TODO: for compatibility with existing functional tests, don't suggest removing a dep that is declared
        //  compileOnly, but I'm not convinced this is what we want long-term.
        emptySet()
      } else if (theBucket == Bucket.NONE) {
        Advice.ofRemove(
          coordinates = coordinates,
          fromConfiguration = location.configurationName
        ).intoSet()
      } else if (theBucket == Bucket.RUNTIME_ONLY) {
        // TODO: for compatibility with existing functional tests, don't suggest changing a dep to runtimeOnly
        //  but I'm not convinced this is what we want long-term.
        emptySet()
      } else {
        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = location.configurationName,
          toConfiguration = theBucket.value
        ).intoSet()
      }
    }
  }

  private object MultiBucket {
    fun reduce(coordinates: Coordinates, usages: Set<Usage>, location: Location): Set<Advice> {
      return usages.asSequence()
        // In a multi-bucket, single-location scenario, if any of the buckets is NONE, ignore it, because really what
        // we're doing is _changing_ the declaration to something variant-specific.
        .filterUsed()
        .mapToConfiguration()
        .map { toConfiguration ->
          Advice.ofChange(
            coordinates = coordinates,
            fromConfiguration = location.configurationName,
            toConfiguration = toConfiguration
          )
        }
        .toSortedSet()
    }
  }
}

private class MultiLocationTransform(
  private val coordinates: Coordinates,
  private val locations: Set<Location>
) : Usage.Transform {
  override fun reduce(usages: Set<Usage>): Set<Advice> {
    return if (usages.size == 1 || countBuckets(usages) == 1) {
      SingleBucket.reduce(coordinates, usages.first(), locations)
    } else {
      MultiBucket.reduce(coordinates, usages, locations)
    }
  }

  private object SingleBucket {
    fun reduce(coordinates: Coordinates, usage: Usage, locations: Set<Location>): Set<Advice> {
      val theBucket = usage.bucket
      val anyMain = locations.any { it.variant == Variant.MAIN }

      return locations.asSequence()
        .mapNotNull { location ->
          if (theBucket == Bucket.NONE) {
            Advice.ofRemove(
              coordinates = coordinates,
              fromConfiguration = location.configurationName
            )
          } else if (location.variant == Variant.MAIN) {
            // Don't change the main declaration
            null
          } else if (anyMain) {
            // If any location is the "main" (w/o variant) location, then we just remove variant-specific locations
            Advice.ofRemove(
              coordinates = coordinates,
              fromConfiguration = location.configurationName
            )
          } else {
            Advice.ofChange(
              coordinates = coordinates,
              fromConfiguration = location.configurationName,
              toConfiguration = theBucket.value
            )
          }
        }
        .toSortedSet()
    }
  }

  private object MultiBucket {
    fun reduce(coordinates: Coordinates, usages: Set<Usage>, locations: Set<Location>): Set<Advice> {
      val advice = sortedSetOf<Advice>()
      val leftoverUsages = usages.toMutableSet()
      val leftoverLocations = mutableSetOf<Location>()

      locations.mapNotNullTo(advice) { location ->
        val locationVariant = location.variant
        // variant-specific declaration
        val locationVariantUsage = usages.find { it.variant == locationVariant.value }

        if (locationVariantUsage != null) {
          leftoverUsages -= locationVariantUsage

          if (locationVariantUsage.bucket == Bucket.NONE) {
            Advice.ofRemove(
              coordinates = coordinates,
              fromConfiguration = location.configurationName
            )
          } else {
            Advice.ofChange(
              coordinates = coordinates,
              fromConfiguration = location.configurationName,
              toConfiguration = locationVariantUsage.toConfiguration()
            )
          }
        } else {
          // null usage implies we need to change this declaration to a variant-specific configuration
          leftoverLocations.add(location)
          null
        }
      }

      // By the time we reach here, we should be out of variant-specific declarations. All that is left is main
      leftoverUsages.mapTo(advice) { usage ->
        val toConfiguration = usage.toConfiguration()
        val theLocation = leftoverLocations.find { it.variant == Variant.MAIN }!!
        leftoverLocations -= theLocation

        Advice.ofChange(
          coordinates = coordinates,
          fromConfiguration = theLocation.configurationName,
          toConfiguration = toConfiguration
        )
      }

      return advice
    }
  }
}

private fun countBuckets(usages: Set<Usage>): Int = usages.mapToSet { it.bucket }.size

/** e.g., "debug" + "implementation" -> "debugImplementation" */
private fun Sequence<Usage>.mapToConfiguration() = map { it.toConfiguration() }

/** e.g., "debug" + "implementation" -> "debugImplementation" */
private fun Usage.toConfiguration(): String {
  return if (Variant(variant) == Variant.MAIN) {
    // "main" + "api" -> "api"
    bucket.value
  } else {
    // "test" + "implementation" -> "testImplementation"
    "${variant}${bucket.value.capitalizeSafely()}"
  }
}

private fun Sequence<Usage>.filterUsed() = filterNot { it.bucket == Bucket.NONE }
