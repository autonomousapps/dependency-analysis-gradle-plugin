// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.autonomousapps.BuildHealthPlugin
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.DependencyAnalysisPlugin
import com.autonomousapps.Flags.AUTO_APPLY
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.internal.artifacts.Resolver
import com.autonomousapps.internal.artifacts.Resolver.Companion.interProjectResolver
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.log
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.services.GlobalDslService
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.register

// TODO(tsr): inline
internal const val DEPENDENCY_ANALYSIS_PLUGIN = DependencyAnalysisPlugin.ID

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
    artifact = DagpArtifacts.Kind.PROJECT_HEALTH,
  )
  private val combinedGraphResolver = interProjectResolver(
    project = project,
    artifact = DagpArtifacts.Kind.COMBINED_GRAPH,
  )
  private val resolvedDepsResolver = interProjectResolver(
    project = project,
    artifact = DagpArtifacts.Kind.RESOLVED_DEPS,
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

    val computeDuplicatesTask = tasks.register<ComputeDuplicateDependenciesTask>("computeDuplicateDependencies") {
      resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
      output.set(paths.duplicateDependenciesPath)
    }

    tasks.register<PrintDuplicateDependenciesTask>("printDuplicateDependencies") {
      duplicateDependenciesReport.set(computeDuplicatesTask.flatMap { it.output })
    }

    tasks.register<ComputeAllDependenciesTask>("computeAllDependencies") {
      resolvedDependenciesReports.setFrom(resolvedDepsResolver.artifactFilesProvider())
      output.set(paths.allLibsVersionsTomlPath)
    }

    val generateBuildHealthTask = tasks.register<GenerateBuildHealthTask>("generateBuildHealth") {
      projectHealthReports.setFrom(adviceResolver.internal.map { it.artifactsFor("json").artifactFiles })
      reportingConfig.set(dagpExtension.reportingHandler.config())
      projectCount.set(allprojects.size)
      dslKind.set(DslKind.from(buildFile))
      dependencyMap.set(dagpExtension.dependenciesHandler.map)
      useTypesafeProjectAccessors.set(dagpExtension.useTypesafeProjectAccessors)

      output.set(paths.buildHealthPath)
      consoleOutput.set(paths.consoleReportPath)
      outputFail.set(paths.shouldFailPath)
    }

    tasks.register<BuildHealthTask>("buildHealth") {
      shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      buildHealth.set(generateBuildHealthTask.flatMap { it.output })
      consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
      printBuildHealth.set(dagpExtension.reportingHandler.printBuildHealth.orElse(printBuildHealth()))
      postscript.set(dagpExtension.reportingHandler.postscript)
    }

    tasks.register<GenerateWorkPlan>("generateWorkPlan") {
      buildPath.set(buildPath(combinedGraphResolver.internal.name))
      combinedProjectGraphs.setFrom(combinedGraphResolver.internal.map { it.artifactsFor("json").artifactFiles })
      outputDirectory.set(paths.workPlanDir)
    }

    // Add a dependency from the root project all projects (including itself).
    val combinedGraphPublisher = interProjectPublisher(
      project = project,
      artifact = DagpArtifacts.Kind.COMBINED_GRAPH,
    )
    val projectHealthPublisher = interProjectPublisher(
      project = this,
      artifact = DagpArtifacts.Kind.PROJECT_HEALTH,
    )
    val resolvedDependenciesPublisher = interProjectPublisher(
      project = this,
      artifact = DagpArtifacts.Kind.RESOLVED_DEPS,
    )

    allprojects.forEach { p ->
      dependencies.let { d ->
        d.add(combinedGraphPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(projectHealthPublisher.declarableName, d.project(mapOf("path" to p.path)))
        d.add(resolvedDependenciesPublisher.declarableName, d.project(mapOf("path" to p.path)))
      }
    }
  }

  private fun Resolver<DagpArtifacts>.artifactFilesProvider(): Provider<FileCollection> =
    internal.map { c ->
      c.incoming.artifactView {
        // Not all projects in the build will have DAGP applied, meaning they won't have any artifact to consume.
        // Setting `lenient(true)` means we can still have a dependency on those projects, and not fail this task when
        // we find nothing there.
        lenient(true)
      }.artifacts.artifactFiles
    }
}
