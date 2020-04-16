package com.autonomousapps

import com.autonomousapps.internal.getPluginAdvicePath
import com.autonomousapps.tasks.RedundantProjectAlertTask
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register

// TODO move this elsewhere
internal class RedundantPluginSubPlugin(
  private val project: Project,
  private val extension: DependencyAnalysisExtension
) {

  fun configure() {
    project.configureRedundantJvmPlugin()
  }

  private fun Project.configureRedundantJvmPlugin() {
    val redundantProjectTask = tasks.register<RedundantProjectAlertTask>("redundantProjectAlert") {
      javaFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.java")
      })
      kotlinFiles.setFrom(project.fileTree(projectDir).matching {
        include("**/*.kt")
      })
      chatty.set(extension.chatty)
      output.set(layout.buildDirectory.file(getPluginAdvicePath()))
    }

    // Add this as an outgoing artifact
    val advicePluginsReportsConf = configurations.create("advicePluginsReportProducer") {
      isCanBeResolved = false
    }
    artifacts {
      add(advicePluginsReportsConf.name, layout.buildDirectory.file(getPluginAdvicePath())) {//redundantProjectTask.map { it.output }) {//
        builtBy(redundantProjectTask)
      }
    }
    // Add project dependency on root project to this project, with our new configuration
    rootProject.dependencies {
      add("advicePluginsReportConsumer", project(this@configureRedundantJvmPlugin.path, advicePluginsReportsConf.name))
    }
  }
}
