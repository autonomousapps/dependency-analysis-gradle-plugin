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
  /**
   * Configuration for the optional package-naming heuristic (Strategy 2). When
   * [PackageHeuristicSettings.enabled] is false (the default), only type-usage-backed matching
   * (Strategy 1) is applied.
   */
  private val heuristic: PackageHeuristicSettings = PackageHeuristicSettings.DISABLED,
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

    // Strategy 2: Package heuristic matching (opt-in; disabled by default).
    // For project deps like ":appian-libraries:end-user-reporting:end-user-reporting-migration"
    // match against classes whose package contains segments from the dep identifier.
    // For external deps like "com.appian:eng-feature-toggles-client"
    // match against classes whose package relates to the group/artifact.
    if (heuristic.enabled && matchesByPackageHeuristic(depIdentifier, report)) return true

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
    return matchesByPackageHeuristic(depIdentifier, report, heuristic)
  }

  /**
   * Plain, serialization-free settings for the package heuristic. Mirrors
   * [com.autonomousapps.extension.RuntimeUsageHandler.Config] but is decoupled from Gradle types so
   * it can be constructed directly in tests and in worker actions.
   */
  internal data class PackageHeuristicSettings(
    val enabled: Boolean,
    /** Prefixes stripped from external-dependency artifact names before matching. */
    val stripPrefixes: Set<String>,
    /** Suffixes stripped from dependency artifact/module names before matching. */
    val stripSuffixes: Set<String>,
    /** For project deps, number of leading path segments to skip. */
    val skipLeadingSegments: Int,
    /** Words ignored when producing match segments. */
    val stopwords: Set<String>,
    /** Minimum length for a segment to be considered a match candidate. */
    val minSegmentLength: Int,
  ) {
    internal companion object {
      val DISABLED: PackageHeuristicSettings = PackageHeuristicSettings(
        enabled = false,
        stripPrefixes = emptySet(),
        stripSuffixes = emptySet(),
        skipLeadingSegments = 0,
        stopwords = emptySet(),
        minSegmentLength = 4,
      )
    }
  }

  companion object {
    /**
     * Returns true if [depIdentifier]'s configured package segments match any runtime-referenced
     * class package or `@ComponentScan` package in [report], per [settings]. Returns false when the
     * heuristic is disabled. This is the reusable core of Strategy 2, callable outside the filter.
     */
    internal fun matchesByPackageHeuristic(
      depIdentifier: String,
      report: RuntimeDepsReport,
      settings: PackageHeuristicSettings,
    ): Boolean {
      if (!settings.enabled) return false
      val depPackageSegments = extractPackageSegments(depIdentifier, settings)
      if (depPackageSegments.isEmpty()) return false

      for (fqcn in report.runtimeReferencedClasses) {
        val classPackage = fqcn.substringBeforeLast('.').lowercase()
        if (depPackageSegments.any { segment -> classPackage.contains(segment) }) return true
      }
      for (pkg in report.componentScanPackages) {
        val pkgLower = pkg.lowercase()
        if (depPackageSegments.any { segment -> pkgLower.contains(segment) }) return true
      }
      return false
    }

    /**
     * Extract meaningful package segments from a dependency identifier, driven by [settings].
     *
     * The structural logic is generic: for project dependencies (`:group:module:artifact`) it takes
     * the path segments after [PackageHeuristicSettings.skipLeadingSegments]; for external modules
     * (`group:artifact`) it uses the artifact name. It then strips the configured prefixes/suffixes,
     * lowercases, removes dashes, and emits both the concatenated form and the individual words that
     * are at least [PackageHeuristicSettings.minSegmentLength] long and not in
     * [PackageHeuristicSettings.stopwords].
     *
     * Example with `stripPrefixes=["appian-","eng-"]`, `skipLeadingSegments=1`,
     * `stopwords=["appian","libraries"]`:
     * - `:appian-libraries:end-user-reporting:end-user-reporting-migration` → ["enduserreporting", "enduser", "reporting", "migration", ...]
     * - `com.appian:eng-feature-toggles-client` → ["featuretoggles", "feature", "toggles"]
     */
    internal fun extractPackageSegments(
      depIdentifier: String,
      settings: PackageHeuristicSettings,
    ): Set<String> {
      val segments = mutableSetOf<String>()
      val minLen = settings.minSegmentLength

      val rawParts: List<String> = if (depIdentifier.startsWith(":")) {
        // Project dependency: take the meaningful path segment(s) after any leading grouping segments.
        depIdentifier.split(":")
          .filter { it.isNotBlank() }
          .drop(settings.skipLeadingSegments)
      } else {
        // External module: use artifact name, stripping configured prefixes/suffixes.
        var artifact = depIdentifier.substringAfter(":")
        for (suffix in settings.stripSuffixes) artifact = artifact.removeSuffix(suffix)
        for (prefix in settings.stripPrefixes) artifact = artifact.removePrefix(prefix)
        listOf(artifact)
      }

      for (part in rawParts) {
        var cleaned = part
        for (suffix in settings.stripSuffixes) cleaned = cleaned.removeSuffix(suffix)

        // Add the full concatenated form (e.g., "maintenancewindow")
        val concatenated = cleaned.replace("-", "").lowercase()
        if (concatenated.length >= minLen && concatenated !in settings.stopwords) {
          segments.add(concatenated)
        }

        // Also add individual dash-separated words (e.g., "maintenance", "window")
        for (word in cleaned.split("-")) {
          val w = word.lowercase()
          if (w.length >= minLen && w !in settings.stopwords) {
            segments.add(w)
          }
        }
      }

      return segments
    }
  }
}
