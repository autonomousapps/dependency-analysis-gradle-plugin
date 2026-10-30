// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.graph.Graphs.lowestCommonAncestor
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.kmp.SourceGraph
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

/**
 * Helps build multiple [DependencyTraceReport]s for a [ComputeUsagesTask][com.autonomousapps.tasks.ComputeUsagesTask]
 * pass. For most projects, there will be only a single report per variant or source set, but for KMP projects, we emit
 * separate reports for the compilation under analysis as well as any associated `commonMain` and `commonTest` source
 * sets.
 */
internal class DependencyTraceReports(
  private val buildType: String?,
  private val flavor: String?,
  private val kmpSourceGraph: SourceGraph?,
) {

  // TODO(tsr): this results in one report per dependency, which seems weird (but works). Can we reduce this to one
  //  report per source kind? is that worthwhile?
  private val builders = sortedMapOf<Coordinates, DependencyTraceReport.Builder>()

  val reports: Set<DependencyTraceReport> get() = builders.values.mapToOrderedSet { it.build() }

  /**
   * This must be called for each [coordinates] *before* [bucket] is called. There can be many reasons, there can be
   * only one bucket.
   */
  fun reason(sourceKind: SourceKind, coordinates: Coordinates, kind: DependencyTraceReport.Kind, reason: Reason) {
    // Get or create a new builder, mutating it as well.
    val builder = builders[coordinates] ?: DependencyTraceReport.Builder(buildType, flavor, kmpSourceGraph)
    builder.withSourceKind(sourceKind)
    builder[coordinates, kind] = reason
    builders[coordinates] = builder
  }

  /**
   * This must be called for each [coordinates] *after* [reason] is called. There can be many reasons, there can be only
   * one bucket.
   */
  fun bucket(coordinates: Coordinates, kind: DependencyTraceReport.Kind, bucket: Bucket) {
    val builder = builders[coordinates] ?: error("No report builder for $coordinates")
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
    private val kmpSourceGraph: SourceGraph?,
  ) {
    private val sourceKinds = mutableSetOf<SourceKind>()

    private val dependencies = mutableMapOf<Coordinates, Trace>()
    private val annotationProcessors = mutableMapOf<Coordinates, Trace>()
    private val reasons = mutableMapOf<Coordinates, MutableSet<Reason>>()

    fun withSourceKind(sourceKind: SourceKind) {
      sourceKinds.add(sourceKind)
    }

    operator fun set(coordinates: Coordinates, kind: Kind, reason: Reason) {
      if (kind == Kind.ANNOTATION_PROCESSOR) {
        handleAnnotationProcessor(coordinates, reason)
      } else {
        handleDependency(coordinates, reason)
      }
    }

    operator fun set(coordinates: Coordinates, kind: Kind, bucket: Bucket) {
      if (kind == Kind.ANNOTATION_PROCESSOR) {
        handleAnnotationProcessor(coordinates, bucket)
      } else {
        handleDependency(coordinates, bucket)
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

    fun build(): DependencyTraceReport {
      val sourceKind = if (sourceKinds.size == 1) {
        sourceKinds.first()
      } else if (kmpSourceGraph != null) {
        val sourceSetNames = sourceKinds.map { it.name }
        val lca = kmpSourceGraph.graph.lowestCommonAncestor(KmpSourceKind.COMMON_MAIN_NAME, sourceSetNames)
        sourceKinds.first { it.name == lca }
      } else {
        error("Could not determine sourceKind for dependency trace report. Available SourceKinds: $sourceKinds")
      }

      return DependencyTraceReport(
        buildType = buildType,
        flavor = flavor,
        sourceKind = sourceKind,
        dependencies = dependencies.withReasons(),
        annotationProcessors = annotationProcessors.withReasons(),
      )
    }

    private fun Map<Coordinates, Trace>.withReasons(): Set<Trace> = values.mapToSet {
      val reasons = reasons[it.coordinates] ?: error("No reasons found for ${it.coordinates}")
      Trace(it.coordinates, it.bucket, reasons)
    }
  }
}
