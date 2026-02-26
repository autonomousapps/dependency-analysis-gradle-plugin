// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.autonomousapps.BuildHealthPlugin
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.Flags.AUTO_APPLY
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.artifacts.Resolver.Companion.interProjectResolver
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
  private val projectMetadataResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PROJECT_METADATA,
  )
  private val combinedGraphResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.COMBINED_GRAPH,
  )
  private val resolvedDepsResolver = interProjectResolver(
    project = project,
    artifactDescription = DagpArtifacts.Kind.RESOLVED_DEPS,
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

    val computeDuplicatesTask = tasks.register("computeDuplicateDependencies", ComputeDuplicateDependenciesTask::class.java) {
      it.resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
      it.output.set(paths.duplicateDependenciesPath)
    }

    tasks.register("printDuplicateDependencies", PrintDuplicateDependenciesTask::class.java) {
      it.duplicateDependenciesReport.set(computeDuplicatesTask.flatMap { it.output })
    }

    tasks.register("computeAllDependencies", ComputeAllDependenciesTask::class.java) {
      it.resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
      it.output.set(paths.allLibsVersionsTomlPath)
    }

    val generateBuildHealthTask = tasks.register("generateBuildHealth", GenerateBuildHealthTask::class.java) { t ->
      t.projectHealthReports.setFrom(adviceResolver.internal.map { it.artifactsFor("json").artifactFiles })
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

    tasks.register("buildHealth", BuildHealthTask::class.java) {
      it.shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      it.buildHealth.set(generateBuildHealthTask.flatMap { it.output })
      it.consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
      it.printBuildHealth.set(dagpExtension.reportingHandler.printBuildHealth.orElse(printBuildHealth()))
      it.postscript.set(dagpExtension.reportingHandler.postscript)
    }

    tasks.register("generateWorkPlan", GenerateWorkPlan::class.java) {
      it.buildPath.set(buildPath(combinedGraphResolver.internal.name))
      it.combinedProjectGraphs.setFrom(combinedGraphResolver.internal.map { it.artifactsFor("json").artifactFiles })
      it.outputDirectory.set(paths.workPlanDir)
    }

    // Add a dependency from the root project to all projects (including itself).
    val combinedGraphPublisher = interProjectPublisher(
      project = this,
      artifactDescription = DagpArtifacts.Kind.COMBINED_GRAPH,
    )
    val projectHealthPublisher = interProjectPublisher(
      project = this,
      artifactDescription = DagpArtifacts.Kind.PROJECT_HEALTH,
    )
    val projectMetadataPublisher = interProjectPublisher(
      project = this,
      artifactDescription = DagpArtifacts.Kind.PROJECT_METADATA,
    )
    val resolvedDependenciesPublisher = interProjectPublisher(
      project = this,
      artifactDescription = DagpArtifacts.Kind.RESOLVED_DEPS,
    )

    allprojects.forEach { p ->
      dependencies.let { d ->
        d.add(combinedGraphPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(projectHealthPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(projectMetadataPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(resolvedDependenciesPublisher.declarableName, d.project(mapOf("path" to p.path)))
      }
    }
  }
}
