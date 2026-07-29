// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.intermediates.RuntimeDepsReport

/**
 * Suppresses "remove" advice for dependencies detected as runtime-used
 * via Spring DI (@ComponentScan, @Bean, @Import) or Liquibase migration references.
 *
 * Uses two strategies to match runtime-referenced classes to dependencies:
 * 1. **Type usage data** (depToClasses): if a dep is known to provide a referenced class, suppress.
 * 2. **Package heuristic**: if a referenced class's package matches the dep identifier (by naming
 *    convention), suppress. This handles the case where the dep isn't in type usage data because
 *    it's only used at runtime.
 */
internal class RuntimeUsageFilter(
  /** Runtime deps reports keyed by project path */
  private val runtimeDepsReports: Map<String, RuntimeDepsReport>,
  /** Map: dependency identifier -> set of class FQCNs it provides (from type usage data) */
  private val depToClasses: Map<String, Set<String>>,
) {

  fun filter(projectAdvices: List<ProjectAdvice>): List<ProjectAdvice> {
    return projectAdvices.map { projectAdvice ->
      val report = runtimeDepsReports[projectAdvice.projectPath]
        ?: return@map projectAdvice

      if (report.runtimeReferencedClasses.isEmpty() && report.componentScanPackages.isEmpty()) {
        return@map projectAdvice
      }

      val filteredAdvice = projectAdvice.dependencyAdvice.filterTo(mutableSetOf()) { advice ->
        !shouldSuppress(advice, report)
      }

      if (filteredAdvice.size == projectAdvice.dependencyAdvice.size) {
        projectAdvice
      } else {
        projectAdvice.copy(dependencyAdvice = filteredAdvice)
      }
    }
  }

  private fun shouldSuppress(advice: Advice, report: RuntimeDepsReport): Boolean {
    if (!advice.isRemove()) return false

    val depIdentifier = advice.coordinates.identifier

    // Strategy 1: Check type usage data (exact match)
    val classesProvidedByDep = depToClasses[depIdentifier]
    if (classesProvidedByDep != null) {
      for (fqcn in report.runtimeReferencedClasses) {
        if (fqcn in classesProvidedByDep) return true
      }
      for (pkg in report.componentScanPackages) {
        val prefix = "$pkg."
        if (classesProvidedByDep.any { it.startsWith(prefix) }) return true
      }
    }

    // Strategy 2: Package heuristic matching
    // For project deps like ":appian-libraries:end-user-reporting:end-user-reporting-migration"
    // match against classes whose package contains segments from the dep identifier.
    // For external deps like "com.appian:eng-feature-toggles-client"
    // match against classes whose package relates to the group/artifact.
    if (matchesByPackageHeuristic(depIdentifier, report)) return true

    return false
  }

  /**
   * Heuristic: match a dependency identifier against runtime-referenced classes by package naming.
   *
   * For project deps (`:foo:bar-java`), we extract "bar" and check if any referenced class
   * has a package containing "bar" (normalized).
   *
   * For module deps (`com.appian:eng-feature-toggles-client`), we extract the artifact name
   * and check if any referenced class's package relates to it.
   */
  private fun matchesByPackageHeuristic(depIdentifier: String, report: RuntimeDepsReport): Boolean {
    val depPackageSegments = extractPackageSegments(depIdentifier)
    if (depPackageSegments.isEmpty()) return false

    // Check runtime-referenced classes
    for (fqcn in report.runtimeReferencedClasses) {
      val classPackage = fqcn.substringBeforeLast('.').lowercase()
      if (depPackageSegments.any { segment -> classPackage.contains(segment) }) {
        return true
      }
    }

    // Check @ComponentScan packages
    for (pkg in report.componentScanPackages) {
      val pkgLower = pkg.lowercase()
      if (depPackageSegments.any { segment -> pkgLower.contains(segment) }) {
        return true
      }
    }

    return false
  }

  companion object {
    /**
     * Extract meaningful package segments from a dependency identifier.
     *
     * - `:appian-libraries:end-user-reporting:end-user-reporting-migration` → ["enduserreporting", "enduser", "reporting", "migration"]
     * - `com.appian:eng-feature-toggles-client` → ["featuretoggles", "feature", "toggles"]
     * - `:appian-libraries:quick-access:quick-access-api` → ["quickaccess", "quick", "access"]
     * - `:appian-libraries:maintenance-window:maintenance-window-java` → ["maintenancewindow", "maintenance", "window"]
     *
     * Returns normalized (lowercase, no dashes) segments that can be matched against package names.
     */
    internal fun extractPackageSegments(depIdentifier: String): Set<String> {
      val segments = mutableSetOf<String>()

      val rawParts: List<String> = if (depIdentifier.startsWith(":")) {
        // Project dependency: take the last meaningful path segment(s)
        depIdentifier.split(":")
          .filter { it.isNotBlank() }
          .drop(1) // skip "appian-libraries" or similar prefix
      } else {
        // External module: use artifact name
        listOf(depIdentifier.substringAfter(":")
          .removeSuffix("-client")
          .removeSuffix("-api")
          .removeSuffix("-impl")
          .removeSuffix("-core")
          .removePrefix("appian-")
          .removePrefix("eng-"))
      }

      for (part in rawParts) {
        val cleaned = part
          .removeSuffix("-java")
          .removeSuffix("-api")
          .removeSuffix("-db")
          .removeSuffix("-contracts")
          .removeSuffix("-impl")
          .removeSuffix("-core")

        // Add the full concatenated form (e.g., "maintenancewindow")
        val concatenated = cleaned.replace("-", "").lowercase()
        if (concatenated.length >= 4) {
          segments.add(concatenated)
        }

        // Also add individual dash-separated words (e.g., "maintenance", "window")
        for (word in cleaned.split("-")) {
          val w = word.lowercase()
          if (w.length >= 4 && w != "appian" && w != "libraries") {
            segments.add(w)
          }
        }
      }

      return segments
    }
  }
}
