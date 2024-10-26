// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.autonomousapps.BuildHealthPlugin
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.DependencyAnalysisPlugin
import com.autonomousapps.Flags.FLAG_AUTO_APPLY
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.internal.artifacts.Resolver.Companion.interProjectResolver
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.log
import com.autonomousapps.services.GlobalDslService
import com.autonomousapps.tasks.BuildHealthTask
import com.autonomousapps.tasks.ComputeDuplicateDependenciesTask
import com.autonomousapps.tasks.GenerateBuildHealthTask
import com.autonomousapps.tasks.PrintDuplicateDependenciesTask
import org.gradle.api.Project
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
    artifact = DagpArtifacts.Kind.PROJECT_HEALTH
  )
  private val resolvedDepsResolver = interProjectResolver(
    project = project,
    artifact = DagpArtifacts.Kind.RESOLVED_DEPS
  )

  fun apply() = project.run {
    logger.log("Adding root project tasks")

    checkFlags()
    checkGuava()
    configureRootProject()
  }

  /** Check for presence of flags that no longer have an effect. */
  private fun Project.checkFlags() {
    val autoApply = providers.gradleProperty(FLAG_AUTO_APPLY)
    if (autoApply.isPresent) {
      if (autoApply.get().toBoolean()) {
        error(
          """
            $FLAG_AUTO_APPLY is set to true, but this has no effect. To automatically apply Dependency Analysis Gradle 
            Plugin  to every project in your build, apply the `${BuildHealthPlugin.ID}` plugin to your settings script.
          """.trimIndent()
        )
      } else {
        logger.warn(
          """
            $FLAG_AUTO_APPLY is set to false, but this is now the only behavior, and the flag has no effect. You should
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
      resolvedDependenciesReports.setFrom(resolvedDepsResolver.internal)
      output.set(paths.duplicateDependenciesPath)
    }

    tasks.register<PrintDuplicateDependenciesTask>("printDuplicateDependencies") {
      duplicateDependenciesReport.set(computeDuplicatesTask.flatMap { it.output })
    }

    val generateBuildHealthTask = tasks.register<GenerateBuildHealthTask>("generateBuildHealth") {
      projectHealthReports.setFrom(adviceResolver.internal.map { it.artifactsFor("json").artifactFiles })
      postscript.set(dagpExtension.reportingHandler.postscript)
      projectCount.set(allprojects.size)
      dslKind.set(DslKind.from(buildFile))
      dependencyMap.set(dagpExtension.dependenciesHandler.map)

      output.set(paths.buildHealthPath)
      consoleOutput.set(paths.consoleReportPath)
      outputFail.set(paths.shouldFailPath)
    }

    tasks.register<BuildHealthTask>("buildHealth") {
      shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
      printBuildHealth.set(printBuildHealth())
      postscript.set(dagpExtension.reportingHandler.postscript)
    }

    // Add a dependency from the root project all projects (including itself).
    val projectHealthPublisher = interProjectPublisher(
      project = this,
      artifact = DagpArtifacts.Kind.PROJECT_HEALTH
    )
    val resolvedDependenciesPublisher = interProjectPublisher(
      project = this,
      artifact = DagpArtifacts.Kind.RESOLVED_DEPS
    )

    allprojects.forEach { p ->
      dependencies.run {
        add(projectHealthPublisher.declarableName, project(p.path))
        add(resolvedDependenciesPublisher.declarableName, project(p.path))
      }
    }
  }
}
