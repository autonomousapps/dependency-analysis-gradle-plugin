package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.SourceSetKind

internal data class DependencyTraceReport(
  val buildType: String?,
  val flavor: String?,
  val variant: String,
  val kind: SourceSetKind,
  val dependencies: Set<Trace>,
  val annotationProcessors: Set<Trace>
) {

  data class Trace(
    val coordinates: Coordinates,
    val bucket: Bucket,
    val reasons: Set<Reason>
  )

  class Builder(
    private val buildType: String?,
    private val flavor: String?,
    private val variant: String,
    private val kind: SourceSetKind
  ) {

    private val dependencies = mutableMapOf<Coordinates, Trace>()
    private val annotationProcessors = mutableMapOf<Coordinates, Trace>()

    operator fun set(coordinates: Coordinates, trace: Pair<Bucket, Reason>) {
      val (bucket, reason) = trace
      if (bucket == Bucket.ANNOTATION_PROCESSOR || reason == Reason.UNUSED_ANNOTATION_PROCESSOR) {
        handleAnnotationProcessor(coordinates, bucket, reason)
      } else {
        handleDependency(coordinates, bucket, reason)
      }
    }

    private fun handleDependency(coordinates: Coordinates, bucket: Bucket, reason: Reason) {
      handle(dependencies, coordinates, bucket, reason)
    }

    private fun handleAnnotationProcessor(coordinates: Coordinates, bucket: Bucket, reason: Reason) {
      check(bucket == Bucket.ANNOTATION_PROCESSOR || reason == Reason.UNUSED_ANNOTATION_PROCESSOR) {
        "Not an annotation processor: $bucket"
      }
      handle(annotationProcessors, coordinates, bucket, reason)
    }

    private fun handle(
      map: MutableMap<Coordinates, Trace>,
      coordinates: Coordinates,
      bucket: Bucket,
      reason: Reason
    ) {
      val currTrace = map[coordinates]
      when (val currBucket = currTrace?.bucket) {
        // new value, set it
        null -> map[coordinates] = Trace(coordinates, bucket, setOf(reason))
        // compatible with current value, merge it
        bucket -> {
          map.merge(coordinates, Trace(coordinates, bucket, setOf(reason))) { acc, inc ->
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
      kind = kind,
      dependencies = dependencies.values.toSet(),
      annotationProcessors = annotationProcessors.values.toSet()
    )
  }
}
