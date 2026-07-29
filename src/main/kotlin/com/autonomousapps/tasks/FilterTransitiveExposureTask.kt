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
      it.outputDir.set(outputDir)
    }
  }

  public interface Parameters : WorkParameters {
    public val projectHealthReports: ConfigurableFileCollection
    public val typeUsageReports: ConfigurableFileCollection
    public val publicClassesReports: ConfigurableFileCollection
    public val runtimeDepsReports: ConfigurableFileCollection
    public val outputDir: DirectoryProperty
  }

  public abstract class Action : WorkAction<Parameters> {
    override fun execute() {
      val outDir = parameters.outputDir.get().asFile
      outDir.deleteRecursively()
      outDir.mkdirs()

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

      val runtimeFilter = RuntimeUsageFilter(
        runtimeDepsReports = runtimeReports,
        depToClasses = depToClasses,
      )

      var totalSuppressed = 0
      var runtimeSuppressed = 0

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

            // Check if any consumer of this project uses classes from the dep
            val isTransitivelyExposed = consumers.any { consumerPath ->
              val consumerUsage = typeUsageByProject[consumerPath] ?: return@any false
              // Check what classes the consumer uses from ALL its project deps
              val allClassesConsumerUses = consumerUsage.projectDependencies.values
                .flatMapTo(mutableSetOf()) { it }
              // If consumer uses any class that the dep provides → transitive exposure
              depPublicClasses.any { it in allClassesConsumerUses }
            }

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
              val shouldSuppress = if (classesProvidedByDep != null) {
                report.runtimeReferencedClasses.any { it in classesProvidedByDep } ||
                  report.componentScanPackages.any { pkg ->
                    classesProvidedByDep.any { it.startsWith("$pkg.") }
                  }
              } else false

              if (shouldSuppress) {
                runtimeSuppressed++
                false
              } else {
                true
              }
            }.toSet()
          }
        } ?: afterTransitiveFilter

        val filteredProjectAdvice = projectAdvice.copy(
          dependencyAdvice = afterRuntimeFilter
        )

        outDir.resolve("${idx}.json").bufferWriteJson(filteredProjectAdvice)
      }

      // Log summary
      println("(dependency analysis) Transitive exposure filter: suppressed $totalSuppressed removal suggestions")
      if (runtimeSuppressed > 0) {
        println("(dependency analysis) Runtime usage filter: suppressed $runtimeSuppressed removal suggestions")
      }
    }
  }
}
