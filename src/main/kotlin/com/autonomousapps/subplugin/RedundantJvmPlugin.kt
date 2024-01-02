// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.autonomousapps.extension.Behavior
import com.autonomousapps.internal.RedundantSubPluginOutputPaths
import com.autonomousapps.tasks.ComputeAdviceTask
import com.autonomousapps.tasks.DetectRedundantJvmPluginTask
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register

class RedundantJvmPlugin(
  private val project: Project,
  private val computeAdviceTask: TaskProvider<ComputeAdviceTask>,
  private val redundantPluginsBehavior: Provider<Behavior>
) {

  private val outputPaths = RedundantSubPluginOutputPaths(project)
  private val javaSource = project.objects.property<Boolean>().convention(false)
  private val kotlinSource = project.objects.property<Boolean>().convention(false)

  fun configure() {
    val detectRedundantJvmPlugin = project.tasks.register<DetectRedundantJvmPluginTask>("detectRedundantJvmPlugin") {
      hasJava.set(javaSource)
      hasKotlin.set(kotlinSource)
      redundantPluginsBehavior.set(this@RedundantJvmPlugin.redundantPluginsBehavior)
      output.set(outputPaths.pluginJvmAdvicePath)
    }

    computeAdviceTask.configure {
      redundantJvmPluginReport.set(detectRedundantJvmPlugin.flatMap { it.output })
    }
  }

  internal fun withJava(java: Provider<Boolean>) {
    javaSource.set(java)
  }

  internal fun withKotlin(kotlin: Provider<Boolean>) {
    kotlinSource.set(kotlin)
  }
}
