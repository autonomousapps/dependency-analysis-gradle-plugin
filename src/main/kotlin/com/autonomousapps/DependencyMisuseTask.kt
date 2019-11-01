@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.Library
import com.autonomousapps.internal.TransitiveDependency
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Produces a report of unused direct dependencies and used transitive dependencies.
 */
open class DependencyMisuseTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of unused direct dependencies and used transitive dependencies"
    }

    @get:InputFile
    val declaredDependencies: RegularFileProperty = objects.fileProperty()

    @get:InputFile
    val usedClasses: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val outputUnusedDependencies: RegularFileProperty = objects.fileProperty()

    @get:OutputFile
    val outputUsedTransitives: RegularFileProperty = objects.fileProperty()

    @TaskAction
    fun action() {
        // Input
        val declaredDependenciesFile = declaredDependencies.get().asFile
        val usedClassesFile = usedClasses.get().asFile

        // Output
        val outputUnusedDependenciesFile = outputUnusedDependencies.get().asFile
        val outputUsedTransitivesFile = outputUsedTransitives.get().asFile

        // Cleanup prior execution
        outputUnusedDependenciesFile.delete()
        outputUsedTransitivesFile.delete()

        val declaredLibraries = declaredDependenciesFile.readText().fromJsonList<Library>()
        val usedClasses = usedClassesFile.readLines()

        val unusedLibs = mutableListOf<String>()
        val usedTransitives = mutableListOf<TransitiveDependency>()
        val usedDirectClasses = mutableListOf<String>()
        declaredLibraries
            // Exclude dependencies with zero class files (such as androidx.legacy:legacy-support-v4)
            .filterNot { it.classes.isEmpty() }
            .forEach { lib ->
                var count = 0
                val classes = mutableListOf<String>()

                lib.classes.forEach { declClass ->
                    // Looking for unused direct dependencies
                    if (!lib.isTransitive) {
                        if (!usedClasses.contains(declClass)) {
                            // Unused class
                            count++
                        } else {
                            // Used class
                            usedDirectClasses.add(declClass)
                        }
                    }

                    // Looking for used transitive dependencies
                    if (lib.isTransitive
                        // Black-listing this one.
                        && lib.identifier != "org.jetbrains.kotlin:kotlin-stdlib"
                        // Assume all these come from android.jar
                        && !declClass.startsWith("android.")
                        && usedClasses.contains(declClass)
                        // Not in the list of used direct dependencies
                        && !usedDirectClasses.contains(declClass)
                    ) {
                        classes.add(declClass)
                    }
                }
                if (count == lib.classes.size) {
                    unusedLibs.add(lib.identifier)
                }
                if (classes.isNotEmpty()) {
                    usedTransitives.add(TransitiveDependency(lib.identifier, classes))
                }
            }

        outputUnusedDependenciesFile.writeText(unusedLibs.joinToString("\n"))
        logger.quiet("Unused dependencies:\n${unusedLibs.joinToString(separator = "\n- ", prefix = "- ")}\n")

        // TODO known issues:
        // 1. org.jetbrains.kotlin:kotlin-stdlib should be excluded TODO or maybe not?
        // TODO 2. generated code might used transitives (such as dagger.android using vanilla dagger; and org.jetbrains:annotations).
        // 3. Some deps might be direct AND transitive, and I don't currently de-dup this. See nl.qbusict:cupboard, which references Context
        // 4. Some deps come from android.jar, and should be excluded
        outputUsedTransitivesFile.writeText(usedTransitives.toJson())
        logger.quiet("Used transitive dependencies:\n${usedTransitives.toPrettyString()}")
    }
}