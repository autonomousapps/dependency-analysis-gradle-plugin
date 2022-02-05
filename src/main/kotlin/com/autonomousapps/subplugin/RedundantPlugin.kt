package com.autonomousapps.subplugin

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.ComputeAdviceTask
import com.autonomousapps.tasks.RedundantPluginAlertTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register

class RedundantPlugin(
  private val project: Project,
  private val computeAdviceTask: TaskProvider<ComputeAdviceTask>,
  private val redundantPluginsBehavior: Provider<Behavior>
) {

  private val outputPaths = RedundantSubPluginOutputPaths(project)
  private val javaSource = project.objects.property<Boolean>().convention(false)
  private val kotlinSource = project.objects.property<Boolean>().convention(false)

  fun configure() {
    val pluginAlertTask = project.tasks.register<RedundantPluginAlertTask>("redundantPluginAlert") {
      hasJava.set(javaSource)
      hasKotlin.set(kotlinSource)
      redundantPluginsBehavior.set(this@RedundantPlugin.redundantPluginsBehavior)
      output.set(outputPaths.pluginJvmAdvicePath)
    }

    computeAdviceTask.configure {
      redundantPluginReport.set(pluginAlertTask.flatMap { it.output })
    }
  }

  internal fun withJava(java: Provider<Boolean>) {
    javaSource.set(java)
  }

  internal fun withKotlin(kotlin: Provider<Boolean>) {
    kotlinSource.set(kotlin)
  }
}
