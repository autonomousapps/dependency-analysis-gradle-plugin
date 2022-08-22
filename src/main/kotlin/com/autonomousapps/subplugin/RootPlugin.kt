package com.autonomousapps.subplugin

import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.Flags.printBuildHealth
import com.autonomousapps.Flags.shouldAutoApply
import com.autonomousapps.getExtension
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.utils.log
import com.autonomousapps.model.declaration.Configurations.CONF_ADVICE_ALL_CONSUMER
import com.autonomousapps.model.declaration.Configurations.CONF_RESOLVED_DEPS_CONSUMER
import com.autonomousapps.tasks.BuildHealthTask
import com.autonomousapps.tasks.ComputeDuplicateDependenciesTask
import com.autonomousapps.tasks.GenerateBuildHealthTask
import com.autonomousapps.tasks.PrintDuplicateDependenciesTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register

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

  private val adviceAllConf = project.createResolvableConfiguration(CONF_ADVICE_ALL_CONSUMER)
  private val resolvedDepsConf = project.createResolvableConfiguration(CONF_RESOLVED_DEPS_CONSUMER)

  fun apply() = project.run {
    logger.log("Adding root project tasks")

    afterEvaluate {
      // Must be inside afterEvaluate to access user configuration
      configureRootProject()
      conditionallyApplyToSubprojects()
    }
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
      apply(plugin = "com.autonomousapps.dependency-analysis")
    }
  }

  /**
   * Root project. Configures lifecycle tasks that aggregates reports across all subprojects.
   */
  private fun Project.configureRootProject() {
    val paths = RootOutputPaths(this)

    val computeDuplicatesTask = tasks.register<ComputeDuplicateDependenciesTask>("computeDuplicateDependencies") {
      dependsOn(resolvedDepsConf)
      resolvedDependenciesReports = resolvedDepsConf
      output.set(paths.duplicateDependenciesPath)
    }

    tasks.register<PrintDuplicateDependenciesTask>("printDuplicateDependencies") {
      duplicateDependenciesReport.set(computeDuplicatesTask.flatMap { it.output })
    }

    val generateBuildHealthTask = tasks.register<GenerateBuildHealthTask>("generateBuildHealth") {
      dependsOn(adviceAllConf)
      projectHealthReports = adviceAllConf
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

  private fun Project.createResolvableConfiguration(confName: String): Configuration =
    configurations.create(confName) {
      isVisible = false
      isCanBeResolved = true
      isCanBeConsumed = false
    }
}
