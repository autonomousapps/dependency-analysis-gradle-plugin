package com.autonomousapps.subplugin

import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.Flags.FLAG_CLEAR_ARTIFACTS
import com.autonomousapps.Flags.FLAG_SILENT_WARNINGS
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.Flags.shouldAutoApply
import com.autonomousapps.getExtension
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.artifacts.Resolver.Companion.interProjectResolver
import com.autonomousapps.internal.utils.log
import com.autonomousapps.tasks.BuildHealthTask
import com.autonomousapps.tasks.ComputeDuplicateDependenciesTask
import com.autonomousapps.tasks.GenerateBuildHealthTask
import com.autonomousapps.tasks.PrintDuplicateDependenciesTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register

internal const val DEPENDENCY_ANALYSIS_PLUGIN = "com.autonomousapps.dependency-analysis"

/**
 * This "plugin" is applied to the root project only.
 */
internal class RootPlugin(private val project: Project) {

  init {
    check(project == project.rootProject) {
      "This plugin must only be applied to the root project. Was ${project.path}."
    }
    DependencyAnalysisExtension.create(project)
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
    configureRootProject()
    conditionallyApplyToSubprojects()
  }

  /** Only apply to all subprojects if user hasn't requested otherwise. See [shouldAutoApply]. */
  private fun Project.conditionallyApplyToSubprojects() {
    if (!shouldAutoApply()) {
      logger.debug("Not applying plugin to all subprojects. User must apply to each manually")
      return
    }

    logger.debug("Applying plugin to all subprojects")
    subprojects {
      logger.debug("Auto-applying to $path.")
      apply(plugin = DEPENDENCY_ANALYSIS_PLUGIN)
    }
  }

  /** Check for presence of flags that no longer have an effect. */
  private fun Project.checkFlags() {
    val clearArtifacts = providers.gradleProperty(FLAG_CLEAR_ARTIFACTS)
    if (clearArtifacts.isPresent) {
      logger.warn(
        "You have ${FLAG_CLEAR_ARTIFACTS}=${clearArtifacts.get()} set. This flag does nothing; you should remove it."
      )
    }

    val silentWarnings = providers.gradleProperty(FLAG_SILENT_WARNINGS)
    if (silentWarnings.isPresent) {
      logger.warn(
        "You have ${FLAG_SILENT_WARNINGS}=${silentWarnings.get()} set. This flag does nothing; you should remove it."
      )
    }
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
      projectHealthReports.setFrom(adviceResolver.internal)
      dslKind.set(DslKind.from(buildFile))
      dependencyMap.set(getExtension().dependenciesHandler.map)

      output.set(paths.buildHealthPath)
      consoleOutput.set(paths.consoleReportPath)
      outputFail.set(paths.shouldFailPath)
    }

    tasks.register<BuildHealthTask>("buildHealth") {
      shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
      printBuildHealth.set(printBuildHealth())
    }
  }
}
