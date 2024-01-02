// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.declaration.Variant
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class Usage(
  val buildType: String?,
  val flavor: String?,
  val variant: Variant,
  val bucket: Bucket,
  val reasons: Set<Reason>
) {

  companion object {
    val BY_VARIANT: Comparator<Usage> = compareBy { it.variant }
  }

  /**
   * Transform the variant-specific [usages][Usage] of a specific dependency, represented by its
   * [coordinates][Coordinates], into a set of [advice][Advice]. This set may have zero or more elements.
   */
  interface Transform {
    fun reduce(usages: Set<Usage>): Set<Advice>
  }
}

internal class UsageBuilder(
  reports: Set<DependencyTraceReport>,
  private val variants: Collection<Variant>
) {

  val dependencyUsages: Map<Coordinates, Set<Usage>>
  val annotationProcessingUsages: Map<Coordinates, Set<Usage>>

  init {
    val theDependencyUsages = mutableMapOf<Coordinates, MutableSet<Usage>>()
    val theAnnotationProcessingUsages = mutableMapOf<Coordinates, MutableSet<Usage>>()

    reports.forEach { report ->
      report.dependencies.forEach { trace ->
        theDependencyUsages.add(report, trace)
      }
      report.annotationProcessors.forEach { trace ->
        theAnnotationProcessingUsages.add(report, trace)
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
      if (theseUsages.size < variants.size) {
        variants.filterNot { variant ->
          theseUsages.any { it.variant == variant }
        }.forEach { missingVariant ->
          theseUsages += Usage(
            buildType = null,
            flavor = null,
            variant = missingVariant,
            bucket = Bucket.NONE,
            reasons = setOf(Reason.Undeclared)
          )
        }
      }
    }
  }

  private fun MutableMap<Coordinates, MutableSet<Usage>>.add(
    report: DependencyTraceReport,
    trace: DependencyTraceReport.Trace
  ) {
    val usage = Usage(
      buildType = report.buildType,
      flavor = report.flavor,
      variant = report.variant,
      bucket = trace.bucket,
      reasons = trace.reasons
    )
    merge(trace.coordinates, mutableSetOf(usage)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }
}
