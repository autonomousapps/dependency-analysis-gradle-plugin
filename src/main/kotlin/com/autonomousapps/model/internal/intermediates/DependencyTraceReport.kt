// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

internal class DependencyTraceReports(
  private val buildType: String?,
  private val flavor: String?
) {

  // key is source set name (jvmMain, commonMain, etc.)
  private val map = sortedMapOf<String, DependencyTraceReport.Builder>()
  private val coordinatesMap = mutableMapOf<Coordinates, String>()

  val reports: Set<DependencyTraceReport> get() {
    return map.values.mapToOrderedSet { it.build() }
  }

  fun put(sourceKind: SourceKind, coordinates: Coordinates, kind: DependencyTraceReport.Kind, reason: Reason) {
    val builder = map[sourceKind.name] ?: DependencyTraceReport.Builder(buildType, flavor, sourceKind)
    builder[coordinates, kind] = reason
    map[sourceKind.name] = builder

    coordinatesMap[coordinates] = sourceKind.name
  }

  fun put(coordinates: Coordinates, kind: DependencyTraceReport.Kind, bucket: Bucket) {
    val key = coordinatesMap[coordinates] ?: error("impossible!")
    val builder = map[key] ?: error("impossible!")
    builder[coordinates, kind] = bucket
  }
}

@JsonClass(generateAdapter = false)
internal data class DependencyTraceReport(
  val buildType: String?,
  val flavor: String?,
  val sourceKind: SourceKind,
  val dependencies: Set<Trace>,
  val annotationProcessors: Set<Trace>,
) : Comparable<DependencyTraceReport> {

  override fun compareTo(other: DependencyTraceReport): Int {
    return compareBy(DependencyTraceReport::sourceKind)
      .thenBy(LexicographicIterableComparator()) { it.dependencies }
      .thenBy(LexicographicIterableComparator()) { it.annotationProcessors }
      .thenComparing(compareBy<DependencyTraceReport, String?>(nullsFirst()) { it.buildType })
      .thenComparing(compareBy<DependencyTraceReport, String?>(nullsFirst()) { it.flavor })
      .compare(this, other)
  }

  @JsonClass(generateAdapter = false)
  data class Trace(
    val coordinates: Coordinates,
    val bucket: Bucket,
    val reasons: Set<Reason> = emptySet(),
  ) : Comparable<Trace> {
    override fun compareTo(other: Trace): Int {
      return compareBy(Trace::coordinates)
        .thenBy(Trace::bucket)
        .thenBy(LexicographicIterableComparator()) { trace -> trace.reasons.map { it.reason } }
        .compare(this, other)
    }
  }

  @JsonClass(generateAdapter = false)
  enum class Kind {
    DEPENDENCY,
    ANNOTATION_PROCESSOR;
  }

  class Builder(
    private val buildType: String?,
    private val flavor: String?,
    val sourceKind: SourceKind,
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
      bucket: Bucket,
    ) {
      if (isBinaryIncompatible(reasons, coordinates)) {
        // If we already know this dependency is binary-incompatible, don't do anything.
        return
      }

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
      reason: Reason,
    ) {
      if (reason is Reason.BinaryIncompatible) {
        // If the new reason is BinaryIncompatible, set it
        map[coordinates] = mutableSetOf(reason)
        dependencies[coordinates] = Trace(coordinates, Bucket.NONE)
      } else if (!isBinaryIncompatible(map, coordinates)) {
        // If we already set the reason to BinaryIncompatible, don't allow any other Reason
        map.merge(coordinates, mutableSetOf(reason)) { acc, inc ->
          acc.apply { addAll(inc) }
        }
      }
    }

    private fun isBinaryIncompatible(
      map: MutableMap<Coordinates, MutableSet<Reason>>,
      coordinates: Coordinates,
    ): Boolean {
      return map[coordinates]?.filterIsInstance<Reason.BinaryIncompatible>()?.isNotEmpty() == true
    }

    fun build(): DependencyTraceReport = DependencyTraceReport(
      buildType = buildType,
      flavor = flavor,
      sourceKind = sourceKind,
      dependencies = dependencies.withReasons(),
      annotationProcessors = annotationProcessors.withReasons()
    )

    private fun Map<Coordinates, Trace>.withReasons(): Set<Trace> = values.mapToSet {
      val reasons = reasons[it.coordinates] ?: error("No reasons found for ${it.coordinates}")
      Trace(it.coordinates, it.bucket, reasons)
    }
  }
}
