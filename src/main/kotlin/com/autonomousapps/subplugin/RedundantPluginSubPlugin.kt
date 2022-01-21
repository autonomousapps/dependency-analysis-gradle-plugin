@file:Suppress("UnstableApiUsage")

package com.autonomousapps.subplugin

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.AdviceSubprojectAggregationTask
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal class RedundantPluginSubPlugin(
  private val project: Project,
  private val hasJava: Provider<Boolean>,
  private val hasKotlin: Provider<Boolean>,
  private val aggregateAdviceTask: TaskProvider<AdviceSubprojectAggregationTask>,
  private val redundantPluginsBehavior: Provider<Behavior>
) {

  private val outputPaths = RedundantSubPluginOutputPaths(project)

  fun configure() {
    project.configureRedundantJvmPlugin()
  }

  private fun Project.configureRedundantJvmPlugin() {
    val pluginAlertTask = tasks.register<RedundantPluginAlertTask>("redundantPluginAlert") {
      hasJava.set(this@RedundantPluginSubPlugin.hasJava)
      hasKotlin.set(this@RedundantPluginSubPlugin.hasKotlin)
      redundantPluginsBehavior.set(this@RedundantPluginSubPlugin.redundantPluginsBehavior)
      output.set(outputPaths.pluginJvmAdvicePath)
    }
    aggregateAdviceTask.configure {
      redundantJvmAdvice.add(pluginAlertTask.flatMap { it.output })
    }
  }
}
