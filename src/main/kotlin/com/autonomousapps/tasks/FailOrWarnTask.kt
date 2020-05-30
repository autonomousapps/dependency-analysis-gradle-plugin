@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.internal.utils.fromJsonSet
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class FailOrWarnTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Task that fails or warns depending on user preference and the advice"
  }

  @get:Input
  abstract val advice: RegularFileProperty

  @TaskAction
  fun action() {
    val adviceReports = advice.get().asFile.readText().fromJsonSet<BuildHealth>()

    val anyUnusedDependencies = adviceReports.any { (_, advices, _) -> advices.any { it.isRemove() } }
    val anyUsedTransitives = adviceReports.any { (_, advices, _) -> advices.any { it.isAdd() } }
    val anyIncorrectConfigurations = adviceReports.any { (_, advices, _) -> advices.any { it.isChange() } }
    val anyUnusedProcs = adviceReports.any { (_, advices, _) -> advices.any { it.isProcessor() } }
    val anyCompileOnly = adviceReports.any { (_, advices, _) -> advices.any { it.isCompileOnly() } }
    val anyPluginIssues = adviceReports.any { (_, _, pluginAdvices) -> pluginAdvices.isNotEmpty() }

    val shouldFail = adviceReports.any { it.shouldFail }
    val wereIssues = anyUnusedDependencies ||
      anyUsedTransitives ||
      anyIncorrectConfigurations ||
      anyUnusedProcs ||
      anyCompileOnly ||
      anyPluginIssues

    val msg = "There were build health issues. Please see the report\n${advice.get().asFile.path}"
    when {
      shouldFail -> throw GradleException(msg)
      wereIssues -> logger.warn(msg)
      else -> logger.quiet("There were no dependency issues. Congrats!")
    }
  }
}
