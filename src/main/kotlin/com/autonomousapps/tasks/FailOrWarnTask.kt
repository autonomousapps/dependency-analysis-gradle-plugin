@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

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
    abstract val failOnAny: Property<Boolean>

    @get:Input
    abstract val failOnUnusedDependencies: Property<Boolean>

    @get:Input
    abstract val failOnUsedTransitiveDependencies: Property<Boolean>

    @get:Input
    abstract val failOnIncorrectConfiguration: Property<Boolean>

    @TaskAction
    fun action() {
        val adviceReports = advice.get().asFile.readText().fromJsonMapList<String, Advice>()

        val anyUnusedDependencies = adviceReports.any { (_, advices) -> advices.any { it.isRemove() } }
        val anyUsedTransitives = adviceReports.any { (_, advices) -> advices.any { it.isAdd() } }
        val anyIncorrectConfigurations = adviceReports.any { (_, advices) -> advices.any { it.isChange() } }

        var shouldFail = false
        var wereIssues = false
        val failOnAny = failOnAny.get()
        if (anyUnusedDependencies) {
            wereIssues = true
            if (failOnAny || failOnUnusedDependencies.get()) {
                shouldFail = true
                logger.error("There were unused dependencies.")
            }
        }
        if (anyUsedTransitives) {
            wereIssues = true
            if (failOnAny || failOnUsedTransitiveDependencies.get()) {
                shouldFail = true
                logger.error("There were used transitive dependencies.")
            }
        }
        if (anyIncorrectConfigurations) {
            wereIssues = true
            if (failOnAny || failOnIncorrectConfiguration.get()) {
                shouldFail = true
                logger.error("There were dependencies on the wrong configuration.")
            }
        }

        val msg = "There were dependency issues. Please see the report\n${advice.get().asFile.path}"
        if (shouldFail) {
            throw GradleException(msg)
        } else if (wereIssues) {
            logger.warn(msg)
        }
    }
}
