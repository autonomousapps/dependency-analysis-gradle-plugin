@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.AdviceSubprojectAggregationTask
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

internal class RedundantPluginSubPlugin(
  private val project: Project,
  private val aggregateAdviceTask: TaskProvider<AdviceSubprojectAggregationTask>
) {

  private val outputPaths = RedundantSubPluginOutputPaths(project)

  fun configure() {
    project.configureRedundantJvmPlugin()
  }

  private fun Project.configureRedundantJvmPlugin() {
    val pluginAlertTask = tasks.register<RedundantPluginAlertTask>("redundantPluginAlert") {
      javaFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.java")
      })
      kotlinFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.kt")
      })
      output.set(outputPaths.pluginJvmAdvicePath)
    }
    aggregateAdviceTask.configure {
      redundantJvmAdvice.add(pluginAlertTask.flatMap { it.output })
    }
  }
}
