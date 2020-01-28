@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.Behavior
import com.autonomousapps.Fail
import com.autonomousapps.Ignore
import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.fromJsonMapList
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

    @TaskAction
    fun action() {
        val adviceReports = advice.get().asFile.readText().fromJsonMapList<String, Advice>()

        val anyUnusedDependencies = adviceReports.any { (_, advices) -> advices.any { it.isRemove() } }
        val anyUsedTransitives = adviceReports.any { (_, advices) -> advices.any { it.isAdd() } }
        val anyIncorrectConfigurations = adviceReports.any { (_, advices) -> advices.any { it.isChange() } }

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

        val msg = "There were dependency issues. Please see the report\n${advice.get().asFile.path}"
        when {
            shouldFail -> throw GradleException(msg)
            wereIssues -> logger.warn(msg)
            // TODO: for ignoring issues, we should actually ignore upstream so there isn't even a report (for machine consumption)
            ignoredIssues -> logger.quiet("There were dependency issues, but they're being ignored.")
            else -> logger.quiet("There were no dependency issues. Congrats!")
        }
    }
}
