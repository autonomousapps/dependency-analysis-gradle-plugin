// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.declaration.internal.Bucket
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class Usage(
  val buildType: String?,
  val flavor: String?,
  val sourceKind: SourceKind,
  val bucket: Bucket,
  val reasons: Set<Reason>
) {

  /**
   * Transform the variant-specific [usages][Usage] of a specific dependency, represented by its
   * [coordinates][Coordinates], into a set of [advice][Advice]. This set may have zero or more elements.
   */
  interface Transform {
    fun reduce(usages: Set<Usage>): Set<Advice>
  }

  companion object {
    val BY_VARIANT: Comparator<Usage> = compareBy { it.sourceKind }
  }

  /** @see [SourceKind.runtimeMatches] */
  fun runtimeMatches(classpaths: Collection<String>): Boolean {
    return sourceKind.runtimeMatches(classpaths)
  }
}

internal class UsageBuilder(
  traces: Set<DependencyTraceReport>,
  private val sourceKinds: Collection<SourceKind>,
) {

  val dependencyUsages: Map<Coordinates, Set<Usage>>
  val annotationProcessingUsages: Map<Coordinates, Set<Usage>>

  init {
    val theDependencyUsages = mutableMapOf<Coordinates, MutableSet<Usage>>()
    val theAnnotationProcessingUsages = mutableMapOf<Coordinates, MutableSet<Usage>>()

    traces.forEach { report ->
      report.dependencies.forEach { trace ->
        theDependencyUsages.merge(report, trace)
      }
      report.annotationProcessors.forEach { trace ->
        theAnnotationProcessingUsages.merge(report, trace)
      }
    }

    addMissingVariants(theDependencyUsages)
    addMissingVariants(theAnnotationProcessingUsages)

    dependencyUsages = theDependencyUsages
    annotationProcessingUsages = theAnnotationProcessingUsages
  }

  // The advice computation that follows expects every dependency to be associated with a usage for _each_ variant
  // present in the build. To ensure this is the case, we add usages for missing variants
  // (Bucket.NONE and Reason.UNDECLARED).
  private fun addMissingVariants(map: MutableMap<Coordinates, MutableSet<Usage>>) {
    map.forEach { (_, theseUsages) ->
      if (theseUsages.size < sourceKinds.size) {
        sourceKinds.filterNot { sourceKind ->
          theseUsages.any { it.sourceKind == sourceKind }
        }.forEach { missingVariant ->
          theseUsages += Usage(
            buildType = null,
            flavor = null,
            sourceKind = missingVariant,
            bucket = Bucket.NONE,
            reasons = setOf(Reason.Undeclared)
          )
        }
      }
    }
  }

  private fun MutableMap<Coordinates, MutableSet<Usage>>.merge(
    report: DependencyTraceReport,
    trace: DependencyTraceReport.Trace
  ) {
    val usage = Usage(
      buildType = report.buildType,
      flavor = report.flavor,
      sourceKind = report.sourceKind,
      bucket = trace.bucket,
      reasons = trace.reasons
    )

    val other = keys.find { it.identifier == trace.coordinates.identifier }
    if (other != null) {
      val otherVersion = (other as? ModuleCoordinates)?.resolvedVersion
      val thisVersion = (trace.coordinates as? ModuleCoordinates)?.resolvedVersion
      if (otherVersion != null && thisVersion != null && otherVersion != thisVersion) {
        // This mutates the existing set in place
        val usages = get(other)!!.apply { add(usage) }

        // If the new coordinates are "greater than" the other coordinates, add the new and remove the old
        if (thisVersion > otherVersion) {
          put(trace.coordinates, usages)
          remove(other)
        }

        // We're done
        return
      }
    }

    merge(trace.coordinates, mutableSetOf(usage)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }
}
