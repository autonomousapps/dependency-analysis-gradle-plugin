package com.autonomousapps

import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

internal class RedundantPluginSubPlugin(
  private val project: Project,
  private val extension: DependencyAnalysisExtension
) {

  private val outputPaths = RedundantSubPluginOutputPaths(project)

  fun configure() {
    project.configureRedundantJvmPlugin()
  }

  private fun Project.configureRedundantJvmPlugin() {
    tasks.register<RedundantPluginAlertTask>("redundantProjectAlert") {
      javaFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.java")
      })
      kotlinFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.kt")
      })
      chatty.set(extension.chatty)
      output.set(outputPaths.pluginJvmAdvicePath)
    }
  }
}
