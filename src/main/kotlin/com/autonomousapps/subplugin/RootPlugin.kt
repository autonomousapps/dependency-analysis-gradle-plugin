// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.autonomousapps.BuildHealthPlugin
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.Flags.AUTO_APPLY
import com.autonomousapps.Flags.batchSize
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.artifacts.Resolver.Companion.interProjectResolver
import com.autonomousapps.internal.ProjectBatcher
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.log
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.services.GlobalDslService
import com.autonomousapps.tasks.*
import org.gradle.api.Project

/** This "plugin" is applied to the root project only. */
internal class RootPlugin(private val project: Project) {

  init {
    check(project == project.rootProject) {
      "This plugin must only be applied to the root project. Was ${project.path}."
    }
  }

  private val dagpExtension = DependencyAnalysisExtension.of(project)

  // Don't delete. Registering this has side effects.
  @Suppress("unused")
  private val dslService = GlobalDslService.of(project).apply {
    get().apply {
      setRegisteredOnRoot()
      // Hydrate dependencies map with version catalog entries
      withVersionCatalogs(project)
    }
  }


  private val adviceResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PROJECT_HEALTH,
  )
  private val combinedGraphResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.COMBINED_GRAPH,
  )
  private val projectMetadataResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PROJECT_METADATA,
  )
  private val publicClassesResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PUBLIC_CLASSES,
  )
  private val resolvedDepsResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.RESOLVED_DEPS,
  )
  private val typeUsagesResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.TYPE_USAGE,
  )
  private val runtimeDepsResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.RUNTIME_DEPS,
  )

  fun apply() = project.run {
    logger.log("Adding root project tasks")

    checkFlags()
    checkGuava()
    configureRootProject()
  }

  /** Check for presence of flags that no longer have an effect. */
  private fun Project.checkFlags() {
    val autoApply = providers.gradleProperty(AUTO_APPLY)
    if (autoApply.isPresent) {
      if (autoApply.get().toBoolean()) {
        error(
          """
            $AUTO_APPLY is set to true, but this has no effect. To automatically apply Dependency Analysis Gradle 
            Plugin  to every project in your build, apply the `${BuildHealthPlugin.ID}` plugin to your settings script.
          """.trimIndent()
        )
      } else {
        logger.warn(
          """
            $AUTO_APPLY is set to false, but this is now the only behavior, and the flag has no effect. You should
            remove it from your build scripts.
          """.trimIndent()
        )
      }
    }
  }

  private fun checkGuava() {
    dslService.get().verifyValidGuavaVersion()
  }

  /** Root project. Configures lifecycle tasks that aggregates reports across all subprojects. */
  private fun Project.configureRootProject() {
    val paths = RootOutputPaths(this)
    val batchSize = batchSize(100)

    val computeDuplicatesTask =
      tasks.register("computeDuplicateDependencies", ComputeDuplicateDependenciesTask::class.java) { t ->
        t.resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
        t.output.set(paths.duplicateDependenciesPath)
        t.outputConsole.set(paths.duplicateDependenciesConsolePath)
      }

    tasks.register("printDuplicateDependencies", PrintDuplicateDependenciesTask::class.java) { t ->
      t.duplicateDependenciesReport.set(computeDuplicatesTask.flatMap { it.outputConsole })
    }

    tasks.register("computeAllDependencies", ComputeAllDependenciesTask::class.java) { t ->
      t.resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
      t.output.set(paths.allLibsVersionsTomlPath)
    }

    // Filter transitive exposure false positives before generating build health report.
    // This cross-references type usage data to suppress "remove" advice for deps that
    // downstream consumers access transitively.
    val filterTransitiveExposureTask =
      tasks.register("filterTransitiveExposure", FilterTransitiveExposureTask::class.java) { t ->
        t.projectHealthReports.setFrom(adviceResolver.internal.map { it.artifactsFor("json").artifactFiles })
        t.typeUsageReports.setFrom(typeUsagesResolver.internal.map { it.artifactsFor("json").artifactFiles })
        t.publicClassesReports.setFrom(publicClassesResolver.internal.map { it.artifactsFor("json").artifactFiles })
        t.runtimeDepsReports.setFrom(runtimeDepsResolver.internal.map { it.artifactsFor("json").artifactFiles })
        val runtimeUsageConfig = dagpExtension.runtimeUsageHandler.config()
        t.heuristicEnabled.set(runtimeUsageConfig.enabled)
        t.heuristicStripPrefixes.set(runtimeUsageConfig.stripPrefixes)
        t.heuristicStripSuffixes.set(runtimeUsageConfig.stripSuffixes)
        t.heuristicSkipLeadingSegments.set(runtimeUsageConfig.skipLeadingSegments)
        t.heuristicStopwords.set(runtimeUsageConfig.stopwords)
        t.heuristicMinSegmentLength.set(runtimeUsageConfig.minSegmentLength)
        t.outputDir.set(layout.buildDirectory.dir("dagp-filtered-advice"))
      }

    val generateBuildHealthTask = tasks.register("generateBuildHealth", GenerateBuildHealthTask::class.java) { t ->
      // Use filtered advice (transitive exposure false positives removed).
      // The filter task writes filtered ProjectAdvice JSONs into its output directory.
      t.projectHealthReports.setFrom(
        files(layout.buildDirectory.dir("dagp-filtered-advice")).builtBy(filterTransitiveExposureTask)
      )
      t.projectMetadataReports.setFrom(projectMetadataResolver.internal.map { it.artifactsFor("json").artifactFiles })
      t.reportingConfig.set(dagpExtension.reportingHandler.config())
      t.projectCount.set(allprojects.size)
      t.dslKind.set(DslKind.from(buildFile))
      t.dependencyMap.set(dagpExtension.dependenciesHandler.map)
      t.useTypesafeProjectAccessors.set(dagpExtension.useTypesafeProjectAccessors)
      t.useParenthesesForGroovy.set(dagpExtension.dependenciesHandler.useParenthesesForGroovy)

      t.output.set(paths.buildHealthPath)
      t.consoleOutput.set(paths.consoleReportPath)
      t.outputFail.set(paths.shouldFailPath)
    }

    tasks.register("buildHealth", BuildHealthTask::class.java) { t ->
      t.shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      t.buildHealth.set(generateBuildHealthTask.flatMap { it.output })
      t.consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
      t.printBuildHealth.set(dagpExtension.reportingHandler.printBuildHealth.orElse(printBuildHealth()))
      t.postscript.set(dagpExtension.reportingHandler.postscript)
    }

    val generatePublicTypeUsages =
      tasks.register("generatePublicTypeUsages", GeneratePublicTypeUsageTask::class.java) { t ->
        t.publicClassesReports.from(publicClassesResolver.internal.map { it.artifactsFor("json").artifactFiles })
        t.typeUsageReports.from(typeUsagesResolver.internal.map { it.artifactsFor("json").artifactFiles })
        t.output.set(paths.publicTypeUsagePath)
        t.outputConsole.set(paths.publicTypeUsageConsolePath)
      }

    tasks.register("publicTypeUsage", PublicTypeUsageTask::class.java) { t ->
      t.consoleReport.set(generatePublicTypeUsages.flatMap { it.outputConsole })
    }

    tasks.register("generateWorkPlan", GenerateWorkPlan::class.java) { t ->
      t.buildPath.set(buildPath(combinedGraphResolver.internal.name))
      t.combinedProjectGraphs.setFrom(combinedGraphResolver.internal.map { it.artifactsFor("json").artifactFiles })
      t.outputDirectory.set(paths.workPlanDir)
    }

    // Add a dependency from the root project to all projects (including itself).
    val publishers = DagpArtifacts.Kind.entries.map { kind ->
      interProjectPublisher(this, kind)
    }

    allprojects.forEach { p ->
      dependencies.let { d ->
        publishers.forEach { publisher ->
          d.add(publisher.declarableName, d.project(mapOf("path" to p.path)))
        }
      }
    }

    // Register batch aggregate tasks for memory management on very large builds.
    // These add execution ordering constraints so not all project analyses are in-flight simultaneously.
    val projectPaths = allprojects.map { it.path }.toSet()
    val batches = ProjectBatcher.batch(projectPaths, batchSize)

    if (batches.size > 1) {
      val batchTasks = batches.mapIndexed { index, _ ->
        tasks.register("dagpBatchAggregate$index", BatchAggregateTask::class.java) { t ->
          t.batchIndex.set(index)
          t.outputDir.set(layout.buildDirectory.dir("dagp-batches/batch-$index"))
          t.projectHealthReports.setFrom(
            adviceResolver.internal.map { it.artifactsFor("json").artifactFiles }
          )
          t.projectMetadataReports.setFrom(
            projectMetadataResolver.internal.map { it.artifactsFor("json").artifactFiles }
          )
        }
      }

      // Add sequential ordering between batches
      batchTasks.windowed(2).forEach { (earlier, later) ->
        later.configure { it.mustRunAfter(earlier) }
      }

      // Make generateBuildHealth depend on all batch tasks (ensures all batches complete)
      tasks.named("generateBuildHealth") { t ->
        batchTasks.forEach { batchTask ->
          t.dependsOn(batchTask)
        }
      }
    }
  }
}
