package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Coordinates

internal data class DependencyTraceReport(
  val buildType: String?,
  val flavor: String?,
  val variant: String,
  val dependencies: Set<Trace>
) {

  data class Trace(
    val coordinates: Coordinates,
    val bucket: Bucket,
    val reasons: Set<Reason>
  )

  class Builder(
    private val buildType: String?,
    private val flavor: String?,
    private val variant: String
  ) {

    private val dependencies = mutableMapOf<Coordinates, Trace>()

    operator fun set(coordinates: Coordinates, trace: Pair<Bucket, Reason>) {
      val (bucket, reason) = trace
      val currTrace = dependencies[coordinates]
      when (val currBucket = currTrace?.bucket) {
        // new value, set it
        null -> dependencies[coordinates] = Trace(coordinates, bucket, setOf(reason))
        // compatible with current value, merge it
        bucket -> {
          dependencies.merge(coordinates, Trace(coordinates, bucket, setOf(reason))) { acc, inc ->
            Trace(coordinates, currBucket, acc.reasons + inc.reasons)
          }
        }
        // incompatible, throw
        else -> {
          error(
            """It is an error to try to associate a dependency with more than one bucket.
                | Dependency: $coordinates
                | Buckets: $currBucket (orig), $bucket (new)
                | Reasons: ${currTrace.reasons} (orig), $reason (new)
              """.trimMargin()
          )
        }
      }
    }

    fun build() = DependencyTraceReport(
      buildType = buildType,
      flavor = flavor,
      variant = variant,
      dependencies = dependencies.values.toSet()
    )
  }
}
