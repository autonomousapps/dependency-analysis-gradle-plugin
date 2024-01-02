// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class DependencyTraceReport(
  val buildType: String?,
  val flavor: String?,
  val variant: Variant,
  val dependencies: Set<Trace>,
  val annotationProcessors: Set<Trace>
) {

  @JsonClass(generateAdapter = false)
  data class Trace(
    val coordinates: Coordinates,
    val bucket: Bucket,
    val reasons: Set<Reason> = emptySet()
  )

  @JsonClass(generateAdapter = false)
  enum class Kind {
    DEPENDENCY,
    ANNOTATION_PROCESSOR;
  }

  class Builder(
    private val buildType: String?,
    private val flavor: String?,
    private val variant: Variant
  ) {

    private val dependencies = mutableMapOf<Coordinates, Trace>()
    private val annotationProcessors = mutableMapOf<Coordinates, Trace>()
    private val reasons = mutableMapOf<Coordinates, MutableSet<Reason>>()

    operator fun set(coordinates: Coordinates, kind: Kind, bucket: Bucket) {
      if (kind == Kind.ANNOTATION_PROCESSOR) {
        handleAnnotationProcessor(coordinates, bucket)
      } else {
        handleDependency(coordinates, bucket)
      }
    }

    operator fun set(coordinates: Coordinates, kind: Kind, reason: Reason) {
      if (kind == Kind.ANNOTATION_PROCESSOR) {
        handleAnnotationProcessor(coordinates, reason)
      } else {
        handleDependency(coordinates, reason)
      }
    }

    private fun handleDependency(coordinates: Coordinates, bucket: Bucket) {
      handle(dependencies, coordinates, bucket)
    }

    private fun handleAnnotationProcessor(coordinates: Coordinates, bucket: Bucket) {
      handle(annotationProcessors, coordinates, bucket)
    }

    private fun handle(
      map: MutableMap<Coordinates, Trace>,
      coordinates: Coordinates,
      bucket: Bucket
    ) {
      val currTrace = map[coordinates]
      when (val currBucket = currTrace?.bucket) {
        // new value, set it
        null -> map[coordinates] = Trace(coordinates, bucket)
        // compatible with current value, merge it
        bucket -> {
          map.merge(coordinates, Trace(coordinates, bucket)) { acc, inc ->
            Trace(coordinates, currBucket, acc.reasons + inc.reasons)
          }
        }
        // incompatible, throw
        else -> {
          error(
            """It is an error to try to associate a dependency with more than one bucket.
                | Dependency: $coordinates
                | Buckets: $currBucket (original), $bucket (new)
              """.trimMargin()
          )
        }
      }
    }

    private fun handleDependency(coordinates: Coordinates, reason: Reason) {
      handle(reasons, coordinates, reason)
    }

    private fun handleAnnotationProcessor(coordinates: Coordinates, reason: Reason) {
      handle(reasons, coordinates, reason)
    }

    private fun handle(
      map: MutableMap<Coordinates, MutableSet<Reason>>,
      coordinates: Coordinates,
      reason: Reason
    ) {
      map.merge(coordinates, mutableSetOf(reason)) { acc, inc ->
        acc.apply { addAll(inc) }
      }
    }

    fun build(): DependencyTraceReport = DependencyTraceReport(
      buildType = buildType,
      flavor = flavor,
      variant = variant,
      dependencies = dependencies.withReasons(),
      annotationProcessors = annotationProcessors.withReasons()
    )

    private fun Map<Coordinates, Trace>.withReasons(): Set<Trace> = values.mapToSet {
      val reasons = reasons[it.coordinates] ?: error("No reasons found for ${it.coordinates}")
      Trace(it.coordinates, it.bucket, reasons)
    }
  }
}
