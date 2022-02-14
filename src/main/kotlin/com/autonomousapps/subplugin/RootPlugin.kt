package com.autonomousapps.subplugin

import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.Flags.shouldAutoApply
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.configuration.Configurations.CONF_ADVICE_ALL_CONSUMER
import com.autonomousapps.internal.configuration.createConsumableConfiguration
import com.autonomousapps.internal.utils.log
import com.autonomousapps.tasks.BuildHealthTask
import com.autonomousapps.tasks.GenerateBuildHealthTask
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
  }

  fun apply() = project.run {
    logger.log("Adding root project tasks")

    // All of these must be created immediately, outside of the afterEvaluate block below
    DependencyAnalysisExtension.create(this)
    val adviceAllConf = createConsumableConfiguration(CONF_ADVICE_ALL_CONSUMER)

    afterEvaluate {
      // Must be inside afterEvaluate to access user configuration
      configureRootProject(adviceAllConf = adviceAllConf)
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
  private fun Project.configureRootProject(adviceAllConf: Configuration) {
    val paths = RootOutputPaths(this)

    val generateBuildHealthTask = tasks.register<GenerateBuildHealthTask>("generateBuildHealth") {
      dependsOn(adviceAllConf)
      projectHealthReports = adviceAllConf

      output.set(paths.buildHealthPath)
      consoleOutput.set(paths.consoleReportPath)
      outputFail.set(paths.shouldFailPath)
    }

    tasks.register<BuildHealthTask>("buildHealth") {
      shouldFail.set(generateBuildHealthTask.flatMap { it.outputFail })
      consoleReport.set(generateBuildHealthTask.flatMap { it.consoleOutput })
    }
  }
}
