// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.PublicTypes
import com.autonomousapps.model.internal.intermediates.RuntimeDepsReport
import com.autonomousapps.internal.advice.RuntimeUsageFilter
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Cross-project analysis task that suppresses "remove" advice for dependencies that are
 * transitively exposed to downstream consumers.
 *
 * A dependency D declared in project A is "transitively exposed" if:
 * 1. A downstream project B depends on A, AND
 * 2. B uses classes that are provided by D
 *
 * In this case, removing D from A would break B's compilation even though A itself
 * doesn't directly use D's classes.
 *
 * This task runs at the root level after all per-project analysis is complete.
 */
@CacheableTask
public abstract class FilterTransitiveExposureTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    description = "Filters removal advice for dependencies that are transitively exposed to downstream consumers"
  }

  /** Per-project health reports (ProjectAdvice JSON files). */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectHealthReports: ConfigurableFileCollection

  /** Per-project type usage reports showing what classes each project uses from its deps. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val typeUsageReports: ConfigurableFileCollection

  /** Per-project public classes reports showing what classes each project exposes. */
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val publicClassesReports: ConfigurableFileCollection

  /** Per-project runtime deps reports from FindRuntimeDepsTask. */
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val runtimeDepsReports: ConfigurableFileCollection

  /** Whether the optional runtime-usage package heuristic is enabled. */
  @get:Input
  public abstract val heuristicEnabled: Property<Boolean>

  /** Prefixes to strip from external-dependency artifact names before matching. */
  @get:Input
  public abstract val heuristicStripPrefixes: SetProperty<String>

  /** Suffixes to strip from dependency artifact/module names before matching. */
  @get:Input
  public abstract val heuristicStripSuffixes: SetProperty<String>

  /** For project deps, number of leading path segments to skip. */
  @get:Input
  public abstract val heuristicSkipLeadingSegments: Property<Int>

  /** Words ignored when producing match segments. */
  @get:Input
  public abstract val heuristicStopwords: SetProperty<String>

  /** Minimum length for a segment to be considered a match candidate. */
  @get:Input
  public abstract val heuristicMinSegmentLength: Property<Int>

  /** Include-only coordinate regex patterns for advice (empty = include all). */
  @get:Input
  public abstract val includeCoordinates: ListProperty<String>

  /** Exclude coordinate regex patterns for advice (applied after include). */
  @get:Input
  public abstract val excludeCoordinates: ListProperty<String>

  /** Downstream hops to traverse for transitive-exposure check (>= 1). */
  @get:Input
  public abstract val transitiveDepth: Property<Int>

  /** Output directory for filtered project health reports. */
  @get:OutputDirectory
  public abstract val outputDir: DirectoryProperty

  @TaskAction
  public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.projectHealthReports.setFrom(projectHealthReports)
      it.typeUsageReports.setFrom(typeUsageReports)
      it.publicClassesReports.setFrom(publicClassesReports)
      it.runtimeDepsReports.setFrom(runtimeDepsReports)
      it.heuristicEnabled.set(heuristicEnabled)
      it.heuristicStripPrefixes.set(heuristicStripPrefixes)
      it.heuristicStripSuffixes.set(heuristicStripSuffixes)
      it.heuristicSkipLeadingSegments.set(heuristicSkipLeadingSegments)
      it.heuristicStopwords.set(heuristicStopwords)
      it.heuristicMinSegmentLength.set(heuristicMinSegmentLength)
      it.includeCoordinates.set(includeCoordinates)
      it.excludeCoordinates.set(excludeCoordinates)
      it.transitiveDepth.set(transitiveDepth)
      it.outputDir.set(outputDir)
    }
  }

  public interface Parameters : WorkParameters {
    public val projectHealthReports: ConfigurableFileCollection
    public val typeUsageReports: ConfigurableFileCollection
    public val publicClassesReports: ConfigurableFileCollection
    public val runtimeDepsReports: ConfigurableFileCollection
    public val heuristicEnabled: Property<Boolean>
    public val heuristicStripPrefixes: SetProperty<String>
    public val heuristicStripSuffixes: SetProperty<String>
    public val heuristicSkipLeadingSegments: Property<Int>
    public val heuristicStopwords: SetProperty<String>
    public val heuristicMinSegmentLength: Property<Int>
    public val includeCoordinates: ListProperty<String>
    public val excludeCoordinates: ListProperty<String>
    public val transitiveDepth: Property<Int>
    public val outputDir: DirectoryProperty
  }

  public abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      val outDir = parameters.outputDir.get().asFile
      outDir.deleteRecursively()
      outDir.mkdirs()

      val transitiveDepth = parameters.transitiveDepth.getOrElse(1).coerceAtLeast(1)
      val includeRegexes = parameters.includeCoordinates.getOrElse(emptyList()).map { it.toRegex() }
      val excludeRegexes = parameters.excludeCoordinates.getOrElse(emptyList()).map { it.toRegex() }

      // Include if no include patterns, or any include matches; then drop if any exclude matches.
      fun coordinateIncluded(identifier: String): Boolean {
        val included = includeRegexes.isEmpty() || includeRegexes.any { it.containsMatchIn(identifier) }
        if (!included) return false
        return excludeRegexes.none { it.containsMatchIn(identifier) }
      }

      // 1. Parse all type usage reports
      val typeUsageByProject = parameters.typeUsageReports.files
        .filter { it.exists() && it.length() > 0 }
        .mapNotNull { file ->
          try {
            file.fromJson<AggregateTypeUsageReport>()
          } catch (_: Exception) {
            null
          }
        }
        .associateBy { it.projectPath }

      // 2. Parse all public classes reports (what classes each project provides)
      val publicClassesByProject = parameters.publicClassesReports.files
        .filter { it.exists() && it.length() > 0 }
        .mapNotNull { file ->
          try {
            file.fromJson<PublicTypes>()
          } catch (_: Exception) {
            null
          }
        }
        .associateBy { it.projectPath }

      // 3. Build reverse dependency graph: for each project, who depends on it
      //    We derive this from typeUsageReports: if B reports using classes from A,
      //    then B depends on A.
      val dependedBy = mutableMapOf<String, MutableSet<String>>()
      typeUsageByProject.forEach { (accessingProject, report) ->
        report.projectDependencies.keys.forEach { depProject ->
          dependedBy.getOrPut(depProject) { mutableSetOf() }.add(accessingProject)
        }
      }

      // 4. Build: for each project, what classes does it expose? (from its dependencies)
      //    We use public classes to know what a project provides to its consumers.
      //    If project A has dep D, and D provides classes {C1, C2}, and A exposes these
      //    on its compile classpath, then consumers of A can use C1, C2.
      //
      //    Key insight: if consumer B uses class C1, and C1 comes from D (declared in A),
      //    then D is transitively exposed through A.
      //
      //    We detect this by checking: for each "remove D from A" advice,
      //    does any consumer of A use classes that belong to D?
      //
      //    Since we don't have a direct "dep D provides classes X" mapping at this level,
      //    we use a simpler heuristic: check if the dep project's public classes are
      //    referenced by consumers of the declaring project.

      // 5. Process each project's advice and filter transitive exposures
      val allAdvice = parameters.projectHealthReports.files
        .filter { it.exists() && it.length() > 0 }
        .mapNotNull { file ->
          try {
            file.fromJson<ProjectAdvice>()
          } catch (_: Exception) {
            null
          }
        }

      // 5b. Load runtime deps reports for RuntimeUsageFilter
      val runtimeReports = parameters.runtimeDepsReports.files
        .filter { it.exists() && it.length() > 0 }
        .mapNotNull { file ->
          try {
            file.fromJson<RuntimeDepsReport>()
          } catch (_: Exception) {
            null
          }
        }
        .associateBy { it.projectPath }

      // Build depToClasses map for RuntimeUsageFilter (reverse mapping from type usage)
      val depToClasses = mutableMapOf<String, MutableSet<String>>()
      typeUsageByProject.values.forEach { report ->
        report.projectDependencies.forEach { (depId, classes) ->
          depToClasses.getOrPut(depId) { mutableSetOf() }.addAll(classes)
        }
        report.libraryDependencies.forEach { (depId, classes) ->
          depToClasses.getOrPut(depId) { mutableSetOf() }.addAll(classes)
        }
      }

      val heuristicSettings = RuntimeUsageFilter.PackageHeuristicSettings(
        enabled = parameters.heuristicEnabled.getOrElse(false),
        stripPrefixes = parameters.heuristicStripPrefixes.getOrElse(emptySet()),
        stripSuffixes = parameters.heuristicStripSuffixes.getOrElse(emptySet()),
        skipLeadingSegments = parameters.heuristicSkipLeadingSegments.getOrElse(0),
        stopwords = parameters.heuristicStopwords.getOrElse(emptySet()),
        minSegmentLength = parameters.heuristicMinSegmentLength.getOrElse(4),
      )

      val runtimeFilter = RuntimeUsageFilter(
        runtimeDepsReports = runtimeReports,
        depToClasses = depToClasses,
        heuristic = heuristicSettings,
      )

      var totalSuppressed = 0
      var runtimeSuppressed = 0
      var coordinateFiltered = 0

      allAdvice.forEachIndexed { idx, projectAdvice ->
        val projectPath = projectAdvice.projectPath
        val consumers = dependedBy[projectPath].orEmpty()

        if (projectAdvice.dependencyAdvice.isEmpty()) {
          outDir.resolve("${idx}.json").bufferWriteJson(projectAdvice)
          return@forEachIndexed
        }

        // For removal advice targeting project dependencies, check transitive exposure
        val afterTransitiveFilter = if (consumers.isEmpty()) {
          projectAdvice.dependencyAdvice
        } else {
          projectAdvice.dependencyAdvice.filter { advice ->
            if (!advice.isAnyRemove()) return@filter true // Keep non-removal advice

            val depCoords = advice.coordinates
            // Only check project dependencies (the main source of false positives)
            val depProjectPath = when (depCoords) {
              is ProjectCoordinates -> depCoords.identifier
              is IncludedBuildCoordinates -> depCoords.resolvedProject.identifier
              else -> return@filter true // Not a project dep, keep advice
            }

            // Get the public classes that the dep project provides
            val depPublicClasses = publicClassesByProject[depProjectPath]?.types.orEmpty()
            if (depPublicClasses.isEmpty()) return@filter true // Can't check, keep advice

            // Check if any consumer of this project (up to transitiveDepth hops) uses classes
            // from the dep.
            val isTransitivelyExposed = isUsedByConsumers(
              projectPath = projectPath,
              depPublicClasses = depPublicClasses,
              dependedBy = dependedBy,
              typeUsageByProject = typeUsageByProject,
              maxDepth = transitiveDepth,
            )

            if (isTransitivelyExposed) {
              totalSuppressed++
              false // Filter out this advice (suppress)
            } else {
              true // Keep this advice
            }
          }.toSet()
        }

        // Apply runtime usage filter (Spring DI, Liquibase, etc.)
        val afterRuntimeFilter = runtimeReports[projectPath]?.let { report ->
          if (report.runtimeReferencedClasses.isEmpty() && report.componentScanPackages.isEmpty()) {
            afterTransitiveFilter
          } else {
            afterTransitiveFilter.filter { advice ->
              if (!advice.isAnyRemove()) return@filter true
              val depIdentifier = advice.coordinates.identifier
              val classesProvidedByDep = depToClasses[depIdentifier]
              // Strategy 1: type-usage-backed match.
              val strategy1 = if (classesProvidedByDep != null) {
                report.runtimeReferencedClasses.any { it in classesProvidedByDep } ||
                  report.componentScanPackages.any { pkg ->
                    classesProvidedByDep.any { it.startsWith("$pkg.") }
                  }
              } else false

              // Strategy 2: optional package-naming heuristic (no-op when disabled).
              val shouldSuppress = strategy1 ||
                RuntimeUsageFilter.matchesByPackageHeuristic(depIdentifier, report, heuristicSettings)

              if (shouldSuppress) {
                runtimeSuppressed++
                false
              } else {
                true
              }
            }.toSet()
          }
        } ?: afterTransitiveFilter

        // Apply the coordinate filter over ALL remaining advice (add/remove/change).
        val afterCoordinateFilter =
          if (includeRegexes.isEmpty() && excludeRegexes.isEmpty()) {
            afterRuntimeFilter
          } else {
            afterRuntimeFilter.filterTo(mutableSetOf()) { advice ->
              val keep = coordinateIncluded(advice.coordinates.identifier)
              if (!keep) coordinateFiltered++
              keep
            }
          }

        val filteredProjectAdvice = projectAdvice.copy(
          dependencyAdvice = afterCoordinateFilter
        )

        outDir.resolve("${idx}.json").bufferWriteJson(filteredProjectAdvice)
      }

      // Log summary
      println("(dependency analysis) Transitive exposure filter: suppressed $totalSuppressed removal suggestions")
      if (coordinateFiltered > 0) {
        println("(dependency analysis) Coordinate filter: removed $coordinateFiltered advice entries not matching include/exclude patterns")
      }
      if (runtimeSuppressed > 0) {
        println("(dependency analysis) Runtime usage filter: suppressed $runtimeSuppressed removal suggestions")
      }
    }

    /**
     * Returns true if any consumer of [projectPath], up to [maxDepth] hops away, uses any class in
     * [depPublicClasses]. Traverses the reverse dependency graph [dependedBy] breadth-first. This
     * catches multi-hop transitive-exposure chains like A -> B -> C where C uses classes from a dep
     * declared in A. [maxDepth] of 1 checks only direct consumers.
     */
    private fun isUsedByConsumers(
      projectPath: String,
      depPublicClasses: Set<String>,
      dependedBy: Map<String, Set<String>>,
      typeUsageByProject: Map<String, AggregateTypeUsageReport>,
      maxDepth: Int,
    ): Boolean {
      val visited = mutableSetOf<String>()
      val queue = ArrayDeque<Pair<String, Int>>()
      dependedBy[projectPath]?.forEach { queue.add(it to 1) }

      while (queue.isNotEmpty()) {
        val (current, depth) = queue.removeFirst()
        if (!visited.add(current)) continue

        val consumerUsage = typeUsageByProject[current]
        if (consumerUsage != null) {
          val classesConsumerUses = consumerUsage.projectDependencies.values
            .flatMapTo(mutableSetOf()) { it }
          if (depPublicClasses.any { it in classesConsumerUses }) return true
        }

        if (depth < maxDepth) {
          dependedBy[current]?.forEach { next ->
            if (next !in visited) queue.add(next to depth + 1)
          }
        }
      }
      return false
    }
  }
}
