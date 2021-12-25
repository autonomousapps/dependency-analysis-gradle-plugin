package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates

internal data class Usage(
  val buildType: String?,
  val flavor: String?,
  val variant: String,
  val bucket: Bucket,
  val reasons: Set<Reason>
) {

  /**
   * Transform the variant-specific [usages][Usage] of a specific dependency, represented by its [Coordinates], into a
   * set of [Advice]. This set may have zero or more elements.
   */
  interface Transform {
    fun reduce(usages: Set<Usage>): Set<Advice>
  }
}

internal class UsageBuilder(reports: Set<DependencyTraceReport>) {

  val usages: Map<Coordinates, Set<Usage>>

  init {
    val usages = mutableMapOf<Coordinates, MutableSet<Usage>>()

    reports.forEach { report ->
      report.dependencies.forEach { trace ->
        usages.add(report, trace)
      }
    }

    this@UsageBuilder.usages = usages
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
