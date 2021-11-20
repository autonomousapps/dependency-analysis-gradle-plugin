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

internal class UsageBuilder(reports: Set<DependencyUsageReport>) {

  val usages: Map<Coordinates, Set<Usage>>

  init {
    val usages = mutableMapOf<Coordinates, MutableSet<Usage>>()

    reports.forEach { report ->
      report.annotationProcessorDependencies.forEach { annotationProcessorDependency ->
        usages.add(report, annotationProcessorDependency, Bucket.ANNOTATION_PROCESSOR)
      }
      report.abiDependencies.forEach { abiDependency ->
        usages.add(report, abiDependency, Bucket.API)
      }
      report.implDependencies.forEach { implDependency ->
        usages.add(report, implDependency, Bucket.IMPL)
      }
      report.compileOnlyDependencies.forEach { compileOnlyDependency ->
        usages.add(report, compileOnlyDependency, Bucket.COMPILE_ONLY)
      }
      report.runtimeOnlyDependencies.forEach { runtimeOnlyDependency ->
        usages.add(report, runtimeOnlyDependency, Bucket.RUNTIME_ONLY)
      }
      // report.compileOnlyApiDependencies.forEach { abiDependency ->
      // }
      report.unusedDependencies.forEach { unusedDependency ->
        usages.add(report, unusedDependency, Bucket.NONE)
      }
    }

    this@UsageBuilder.usages = usages
  }

  private fun MutableMap<Coordinates, MutableSet<Usage>>.add(
    report: DependencyUsageReport,
    trace: DependencyUsageReport.Trace,
    bucket: Bucket
  ) {
    val usage = Usage(
      buildType = report.buildType,
      flavor = report.flavor,
      variant = report.variant,
      bucket = bucket,
      reasons = trace.reasons
    )
    merge(trace.coordinates, mutableSetOf(usage)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }
}
