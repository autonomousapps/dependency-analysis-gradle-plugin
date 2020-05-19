@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.Behavior
import com.autonomousapps.Fail
import com.autonomousapps.Ignore
import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.internal.utils.fromJsonSet
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class FailOrWarnTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Task that fails or warns depending on user preference and the advice"
  }

  @get:Input
  abstract val advice: RegularFileProperty

  @get:Input
  abstract val failOnAny: Property<Behavior>

  @get:Input
  abstract val failOnUnusedDependencies: Property<Behavior>

  @get:Input
  abstract val failOnUsedTransitiveDependencies: Property<Behavior>

  @get:Input
  abstract val failOnIncorrectConfiguration: Property<Behavior>

  @get:Input
  abstract val failOnUnusedProcs: Property<Behavior>

  @get:Input
  abstract val failOnCompileOnly: Property<Behavior>

  @TaskAction
  fun action() {
    val adviceReports = advice.get().asFile.readText().fromJsonSet<BuildHealth>()

    val anyUnusedDependencies = adviceReports.any { (_, advices, _) -> advices.any { it.isRemove() } }
    val anyUsedTransitives = adviceReports.any { (_, advices, _) -> advices.any { it.isAdd() } }
    val anyIncorrectConfigurations = adviceReports.any { (_, advices, _) -> advices.any { it.isChange() } }
    val anyUnusedProcs = adviceReports.any { (_, advices, _) -> advices.any { it.isProcessor() } }
    val anyCompileOnly = adviceReports.any { (_, advices, _) -> advices.any { it.isCompileOnly() } }
    val anyPluginIssues = adviceReports.any { (_, _, pluginAdvices) -> pluginAdvices.isNotEmpty() }

    var shouldFail = false
    var wereIssues = false
    var ignoredIssues = false
    val failOnAny = failOnAny.get() is Fail

    val onUnusedBehavior = failOnUnusedDependencies.get()
    if (anyUnusedDependencies) {
      if (onUnusedBehavior is Ignore) {
        ignoredIssues = true
      } else {
        wereIssues = true
      }

      if (failOnAny || onUnusedBehavior is Fail) {
        shouldFail = true
        logger.error("There were unused dependencies.")
      }
    }

    val onUsedTransitivesBehavior = failOnUsedTransitiveDependencies.get()
    if (anyUsedTransitives) {
      if (onUsedTransitivesBehavior is Ignore) {
        ignoredIssues = true
      } else {
        wereIssues = true
      }

      if (failOnAny || onUsedTransitivesBehavior is Fail) {
        shouldFail = true
        logger.error("There were used transitive dependencies.")
      }
    }

    val onIncorrectConfigurationBehavior = failOnIncorrectConfiguration.get()
    if (anyIncorrectConfigurations) {
      if (onIncorrectConfigurationBehavior is Ignore) {
        ignoredIssues = true
      } else {
        wereIssues = true
      }

      if (failOnAny || onIncorrectConfigurationBehavior is Fail) {
        shouldFail = true
        logger.error("There were dependencies on the wrong configuration.")
      }
    }

    val onUnusedProcsBehavior = failOnUnusedProcs.get()
    if (anyUnusedProcs) {
      if (onUnusedProcsBehavior is Ignore) {
        ignoredIssues = true
      } else {
        wereIssues = true
      }

      if (failOnAny || onUnusedProcsBehavior is Fail) {
        shouldFail = true
        logger.error("There were unused annotation processors.")
      }
    }

    val onCompileOnly = failOnCompileOnly.get()
    if (anyCompileOnly) {
      if (onCompileOnly is Ignore) {
        ignoredIssues = true
      } else {
        wereIssues = true
      }

      if (failOnAny || onCompileOnly is Fail) {
        shouldFail = true
        logger.error("Some dependencies could be compile only.")
      }
    }

    // TODO filter
    if (anyPluginIssues) {
      wereIssues = true
      if (failOnAny) {
        shouldFail = true
        logger.error("There were issues with applied plugins.")
      }
    }

    val msg = "There were build health issues. Please see the report\n${advice.get().asFile.path}"
    when {
      shouldFail -> throw GradleException(msg)
      wereIssues -> logger.warn(msg)
      // TODO: for ignoring issues, we should actually ignore upstream so there isn't even a report (for machine consumption)
      ignoredIssues -> logger.quiet("There were dependency issues, but they're being ignored.")
      else -> logger.quiet("There were no dependency issues. Congrats!")
    }
  }
}
