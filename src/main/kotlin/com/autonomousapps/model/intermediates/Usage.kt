package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.SourceSetKind

internal data class Usage(
  val buildType: String?,
  val flavor: String?,
  // TODO V2: coalesce variant + kind into Variant()
  val variant: String,
  val kind: SourceSetKind,

  val bucket: Bucket,
  val reasons: Set<Reason>
) {

  val theVariant = Variant(variant, kind)

  /**
   * Transform the variant-specific [usages][Usage] of a specific dependency, represented by its [Coordinates], into a
   * set of [Advice]. This set may have zero or more elements.
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
          theseUsages.any { it.theVariant == variant }
        }.forEach { missingVariant ->
          theseUsages += Usage(
            buildType = null,
            flavor = null,
            variant = missingVariant.variant,
            kind = missingVariant.kind,
            bucket = Bucket.NONE,
            reasons = setOf(Reason.UNDECLARED)
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
      kind = report.kind,
      bucket = trace.bucket,
      reasons = trace.reasons
    )
    merge(trace.coordinates, mutableSetOf(usage)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }
}
