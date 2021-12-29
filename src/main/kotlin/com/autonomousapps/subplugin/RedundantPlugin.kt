package com.autonomousapps.subplugin

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.ComputeAdviceTask
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class RedundantPlugin(
  private val project: Project,
  private val computeAdviceTask: TaskProvider<ComputeAdviceTask>,
  private val redundantPluginsBehavior: Provider<Behavior>
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
      redundantPluginsBehavior.set(this@RedundantPlugin.redundantPluginsBehavior)
      output.set(outputPaths.pluginJvmAdvicePath)
    }
    computeAdviceTask.configure {
      redundantPluginReport.set(pluginAlertTask.flatMap { it.output })
    }
  }
}
