package com.autonomousapps.subplugin

import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.getExtension
import com.autonomousapps.internal.configuration.Configurations.CONF_ADVICE_ALL_CONSUMER
import com.autonomousapps.internal.configuration.Configurations.CONF_PROJECT_GRAPH_CONSUMER
import com.autonomousapps.internal.configuration.Configurations.CONF_PROJECT_METRICS_CONSUMER
import com.autonomousapps.internal.RootOutputPaths
import com.autonomousapps.internal.configuration.createConsumableConfiguration
import com.autonomousapps.internal.utils.log
import com.autonomousapps.shouldAutoApply
import com.autonomousapps.tasks.*
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
    val projGraphConf = createConsumableConfiguration(CONF_PROJECT_GRAPH_CONSUMER)
    val projMetricsConf = createConsumableConfiguration(CONF_PROJECT_METRICS_CONSUMER)

    afterEvaluate {
      // Must be inside afterEvaluate to access user configuration
      configureRootProject(
        adviceAllConf = adviceAllConf,
        projGraphConf = projGraphConf,
        projMetricsConf = projMetricsConf
      )
      conditionallyApplyToSubprojects()
    }
  }

  /**
   * Only apply to all subprojects if user hasn't requested otherwise. See [DependencyAnalysisExtension.autoApply].
   * TODO update this after removing deprecated DependencyAnalysisExtension.autoApply().
   */
  private fun Project.conditionallyApplyToSubprojects() {
    if (!shouldAutoApply()) {
      logger.debug("Not applying plugin to all subprojects. User must apply to each manually")
      return
    }

    if (getExtension().autoApply.get()) {
      logger.debug("Applying plugin to all subprojects")
      subprojects {
        logger.debug("Auto-applying to $path.")
        apply(plugin = "com.autonomousapps.dependency-analysis")
      }
    } else {
      logger.debug("Not applying plugin to all subprojects. User must apply to each manually")
    }
  }

  /**
   * Root project. Configures lifecycle tasks that aggregates reports across all subprojects.
   */
  private fun Project.configureRootProject(
    adviceAllConf: Configuration,
    projGraphConf: Configuration,
    projMetricsConf: Configuration
  ) {
    val outputPaths = RootOutputPaths(this)

    // Aggregates strict advice from all subprojects
    val strictAdviceTask = tasks.register<StrictAdviceTask>("adviceReport") {
      dependsOn(adviceAllConf)

      adviceAllReports = adviceAllConf

      output.set(outputPaths.strictAdvicePath)
      outputPretty.set(outputPaths.strictAdvicePrettyPath)
    }

    // Produces a graph of the project dependencies
    val graphTask = tasks.register<DependencyGraphAllProjects>("projectGraphReport") {
      dependsOn(projGraphConf) // TODO do I need to depend on the configuration
      graphs = projGraphConf

      outputFullGraphJson.set(outputPaths.mergedGraphJsonPath)
      outputFullGraphDot.set(outputPaths.mergedGraphDotPath)
      outputRevGraphJson.set(outputPaths.mergedGraphRevJsonPath)
      outputRevGraphDot.set(outputPaths.mergedGraphRevDotPath)
      outputRevSubGraphDot.set(outputPaths.mergedGraphRevSubDotPath)
    }

    // Trims strict advice of unnecessary (for compilation) transitive dependencies
    val minimalAdviceTask = tasks.register<MinimalAdviceTask>("minimalAdviceReport") {
      dependsOn(projGraphConf)
      graphs = projGraphConf

      adviceReport.set(strictAdviceTask.flatMap { it.output })
      mergedGraph.set(graphTask.flatMap { it.outputFullGraphJson })
      mergedRevGraph.set(graphTask.flatMap { it.outputRevGraphJson })

      with(getExtension().issueHandler) {
        anyBehavior.set(anyIssue())
        unusedDependenciesBehavior.set(unusedDependenciesIssue())
        usedTransitiveDependenciesBehavior.set(usedTransitiveDependenciesIssue())
        incorrectConfigurationBehavior.set(incorrectConfigurationIssue())
        compileOnlyBehavior.set(compileOnlyIssue())
        unusedProcsBehavior.set(unusedAnnotationProcessorsIssue())
        redundantPluginsBehavior.set(redundantPluginsIssue())
      }

      output.set(outputPaths.minimizedAdvicePath)
      outputPretty.set(outputPaths.minimizedAdvicePrettyPath)
    }

    // Copies either strict or minimal advice to the final advice file, as facade for buildHealth.
    val finalAdviceTask = tasks.register<FinalAdviceTask>("finalAdviceReport") {
      // If strict mode, use the full, unfiltered advice. Else, use minimized advice
      val compAdvice =
        if (getExtension().strictMode.get()) strictAdviceTask.flatMap { it.output }
        else minimalAdviceTask.flatMap { it.output }
      buildHealth.set(compAdvice)

      output.set(outputPaths.finalAdvicePath)
    }

    // Aggregates build metrics from all subprojects
    val measureBuildTask = tasks.register<BuildMetricsTask>("measureBuild") {
      dependsOn(projMetricsConf) // TODO do I need to depend on the configuration
      metrics = projMetricsConf

      output.set(outputPaths.buildMetricsPath)
    }

    // A lifecycle task, always runs. Prints build health results to console
    tasks.register<BuildHealthTask>("buildHealth") {
      adviceReport.set(finalAdviceTask.flatMap { it.output })
      dependencyRenamingMap.set(getExtension().dependencyRenamingMap)
      buildMetricsJson.set(measureBuildTask.flatMap { it.output })
    }

    // Prints ripples to console based on --id value
    tasks.register<RipplesTask>("ripples") {
      dependsOn(projGraphConf) // TODO do I need to depend on the configuration
      graphs = projGraphConf

      buildHealthReport.set(finalAdviceTask.flatMap { it.output })
      graph.set(graphTask.flatMap { it.outputFullGraphJson })
      output.set(outputPaths.ripplesPath)
    }
  }
}