@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Produces a report of unused direct dependencies and used transitive dependencies.
 */
//@CacheableTask
open class DependencyMisuseTask @Inject constructor(
    objects: ObjectFactory,
    private val workerExecutor: WorkerExecutor
) : DefaultTask() {

    init {
        group = "verification"
        description = "Produces a report of unused direct dependencies and used transitive dependencies"
    }

    // TODO can I just depend on the ResolutionResult itself?
    @get:Input
    val configurationName: Property<String> = objects.property(String::class.java)

    @PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    val declaredDependencies: RegularFileProperty = objects.fileProperty()

    @PathSensitive(PathSensitivity.RELATIVE)
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
        val root = project.configurations.getByName(configurationName.get()).incoming.resolutionResult.root

        // Output
        val outputUnusedDependenciesFile = outputUnusedDependencies.get().asFile
        val outputUsedTransitivesFile = outputUsedTransitives.get().asFile

        // Cleanup prior execution
        outputUnusedDependenciesFile.delete()
        outputUsedTransitivesFile.delete()

        val declaredLibraries = declaredDependenciesFile.readText().fromJsonList<Component>()
        val usedClasses = usedClassesFile.readLines()

        val unusedLibs = mutableListOf<String>()
        val usedTransitives = mutableListOf<TransitiveDependency>()
        val usedDirectClasses = mutableListOf<String>()
        declaredLibraries
            // Exclude dependencies with zero class files (such as androidx.legacy:legacy-support-v4)
            .filterNot { it.classes.isEmpty() }
            .forEach { lib ->
                var count = 0
                val classes = sortedSetOf<String>()

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
                if (count == lib.classes.size
                    // Blacklisting all of these
                    && !lib.identifier.startsWith("org.jetbrains.kotlin:kotlin-stdlib")
                ) {
                    unusedLibs.add(lib.identifier)
                }
                if (classes.isNotEmpty()) {
                    usedTransitives.add(TransitiveDependency(lib.identifier, classes))
                }
            }

        // Connect transitive to direct dependencies TODO start cleanup
        val unusedDeps = mutableSetOf<UnusedDirectDependency>()
        unusedLibs.forEach { unusedLib ->
            val resolvedDepResult = root.dependencies.filterIsInstance<ResolvedDependencyResult>().find {
                val identifier = it.selected.id.asString()
                identifier == unusedLib
            }
            resolvedDepResult?.let {
                val unusedDep = relate(it, UnusedDirectDependency(unusedLib, mutableSetOf()), usedTransitives.toSet())
                unusedDeps.add(unusedDep)
            }
        }
        println("Unused direct dependencies:\n${unusedDeps.toPrettyString()}\n")
        // TODO end

        outputUnusedDependenciesFile.writeText(unusedLibs.joinToString("\n"))
        logger.quiet("Unused dependencies report: ${outputUnusedDependenciesFile.path}")
        logger.quiet("Unused dependencies:\n${unusedLibs.joinToString(separator = "\n- ", prefix = "- ")}\n")

        outputUsedTransitivesFile.writeText(usedTransitives.toJson())
        logger.quiet("Used transitive dependencies report: ${outputUsedTransitivesFile.path}")
        logger.quiet("Used transitive dependencies:\n${usedTransitives.toPrettyString()}")
    }

    private fun relate(resolvedDependency: ResolvedDependencyResult, unusedDep: UnusedDirectDependency, transitives: Set<TransitiveDependency>): UnusedDirectDependency {
        resolvedDependency.selected.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach {
            val identifier = it.selected.id.asString()
            if (transitives.map { it.identifier }.contains(identifier)) {
                unusedDep.usedTransitiveDependencies.add(identifier)
            }
            relate(it, unusedDep, transitives)
        }
        return unusedDep
    }
}

// TODO move elsewhere
private data class UnusedDirectDependency(
    /**
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val identifier: String,
    /**
     * If this direct dependency has any transitive dependencies that are used, they will be in this set.
     *
     * In group:artifact form. E.g.,
     * 1. "javax.inject:javax.inject"
     * 2. ":my-project"
     */
    val usedTransitiveDependencies: MutableSet<String>
)
