@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces human- and machine-readable advice on how to modify a project's dependencies in order to have a healthy
 * build.
 */
@CacheableTask
abstract class AdviceTask : DefaultTask() {

    init {
        group = TASK_GROUP_DEP
        description = "Provides advice on how best to declare the project's dependencies"
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val unusedDependenciesReport: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val usedTransitiveDependenciesReport: RegularFileProperty

    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val abiDependenciesReport: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val allDeclaredDependenciesReport: RegularFileProperty

    @get:OutputFile
    abstract val adviceReport: RegularFileProperty

    @TaskAction
    fun action() {
        // Output
        val adviceFile = adviceReport.get().asFile
        adviceFile.delete()

        // Inputs
        val unusedDirectComponents = unusedDependenciesReport.get().asFile.readText().fromJsonList<UnusedDirectComponent>()
        val usedTransitiveComponents = usedTransitiveDependenciesReport.get().asFile.readText().fromJsonList<TransitiveComponent>()
        val abiDeps = abiDependenciesReport.orNull?.asFile?.readText()?.fromJsonList<Dependency>() ?: emptyList()
        val allDeclaredDeps = allDeclaredDependenciesReport.get().asFile.readText().fromJsonList<Artifact>()
            .map { it.dependency }
            .filter { it.configurationName != null }

        // Print to the console three lists:
        // 1. Dependencies that should be removed
        // 2. Dependencies that are already declared and whose configurations should be modified
        // 3. Dependencies that should be added and the configurations on which to add them

        val advisor = Advisor(
            unusedDirectComponents = unusedDirectComponents,
            usedTransitiveComponents = usedTransitiveComponents,
            abiDeps = abiDeps,
            allDeclaredDeps = allDeclaredDeps
        )

        advisor.getRemoveAdvice()?.let {
            logger.quiet("Completely unused dependencies which should be removed:\n$it\n")
        }
        advisor.getChangeAdvice()?.let {
            logger.quiet("Existing dependencies which should be modified to be as indicated:\n$it\n")
        }
        advisor.getAddAdvice()?.let {
            logger.quiet("Transitively used dependencies that should be declared directly as indicated:\n$it\n")
        }

        logger.quiet("See machine-readable report at ${adviceFile.path}")
        adviceFile.writeText(advisor.getAdvices().toJson())
    }
}
