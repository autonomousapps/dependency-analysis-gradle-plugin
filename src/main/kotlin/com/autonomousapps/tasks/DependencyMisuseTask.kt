@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Produces a report of unused direct dependencies and used transitive dependencies.
 */
@CacheableTask
abstract class DependencyMisuseTask : DefaultTask() {

    init {
        group = TASK_GROUP_DEP
        description = "Produces a report of unused direct dependencies and used transitive dependencies"
    }

    /**
     * This is the "official" input for wiring task dependencies correctly, but is otherwise
     * unused.
     */
    @get:Classpath
    lateinit var artifactFiles: FileCollection

    /**
     * This is what the task actually uses as its input.
     */
    @get:Internal
    lateinit var runtimeConfiguration: Configuration

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val declaredDependencies: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val usedClasses: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val usedInlineDependencies: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val usedConstantDependencies: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    abstract val usedAndroidResDependencies: RegularFileProperty

    @get:OutputFile
    abstract val outputUnusedDependencies: RegularFileProperty

    @get:OutputFile
    abstract val outputUsedTransitives: RegularFileProperty

    @get:OutputFile
    abstract val outputHtml: RegularFileProperty

    @TaskAction
    fun action() {
        // Input
        val declaredDependenciesFile = declaredDependencies.get().asFile
        val usedClassesFile = usedClasses.get().asFile
        val usedInlineDependenciesFile = usedInlineDependencies.get().asFile
        val usedConstantDependenciesFile = usedConstantDependencies.get().asFile
        val usedAndroidResourcesFile = usedAndroidResDependencies.orNull?.asFile
        val resolvedComponentResult: ResolvedComponentResult = runtimeConfiguration
            .incoming
            .resolutionResult
            .root

        // Output
        val outputUnusedDependenciesFile = outputUnusedDependencies.get().asFile
        val outputUsedTransitivesFile = outputUsedTransitives.get().asFile
        val outputHtmlFile = outputHtml.get().asFile

        // Cleanup prior execution
        outputUnusedDependenciesFile.delete()
        outputUsedTransitivesFile.delete()
        outputHtmlFile.delete()

        val detector = MisusedDependencyDetector(
            declaredComponents = declaredDependenciesFile.readText().fromJsonList(),
            usedClasses = usedClassesFile.readLines(),
            usedInlineDependencies = usedInlineDependenciesFile.readText().fromJsonList(),
            usedConstantDependencies = usedConstantDependenciesFile.readText().fromJsonList(),
            usedAndroidResDependencies = usedAndroidResourcesFile?.readText()?.fromJsonList(),
            root = resolvedComponentResult
        )
        val dependencyReport = detector.detect()

        // Reports
        outputUnusedDependenciesFile.writeText(dependencyReport.unusedDepsWithTransitives.toJson())
        outputUsedTransitivesFile.writeText(dependencyReport.usedTransitives.toJson())
        logger.debug(
//            """
//            |===Misused Dependencies===
//            |This report contains directly declared dependencies (in your `dependencies {}` block) which are either:
//            | 1. Completely unused; or
//            | 2. Unused except for transitive dependencies which _are_ used.
//            |    These used-transitives are either declared on the `compile` or `api` configurations (or the Maven equivalent)
//            |    of their respective projects. In some cases, it makes sense to simply use these transitive dependencies. In
//            |    others, it may be best to directly declare these transitive dependencies in your build script.
//            |
            """
            |Unused dependencies report:          ${outputUnusedDependenciesFile.path}
            |Used-transitive dependencies report: ${outputUsedTransitivesFile.path}
            |
            |Completely unused dependencies:
            |${if (dependencyReport.completelyUnusedDeps.isEmpty()) "none" else dependencyReport.completelyUnusedDeps.joinToString(
                separator = "\n- ",
                prefix = "- "
            )}
        """.trimMargin()
        )

//        writeHtmlReport(
//            dependencyReport.completelyUnusedDeps,
//            dependencyReport.unusedDepsWithTransitives,
//            dependencyReport.usedTransitives,
//            outputHtmlFile
//        )
    }
}

internal class MisusedDependencyDetector(
    private val declaredComponents: List<Component>,
    private val usedClasses: List<String>,
    private val usedInlineDependencies: List<Dependency>,
    private val usedConstantDependencies: List<Dependency>,
    private val usedAndroidResDependencies: List<Dependency>?,
    private val root: ResolvedComponentResult
) {
    fun detect(): DependencyReport {
        val unusedLibs = mutableListOf<Dependency>()
        val usedTransitives = mutableSetOf<TransitiveComponent>()
        val usedDirectClasses = mutableSetOf<String>()

        declaredComponents
            // Exclude dependencies with zero class files (such as androidx.legacy:legacy-support-v4)
            .filterNot { it.classes.isEmpty() }
            .forEach { component ->
                var count = 0
                val classes = sortedSetOf<String>()

                component.classes.forEach { declClass ->
                    // Looking for unused direct dependencies
                    if (!component.isTransitive) {
                        if (!usedClasses.contains(declClass)) {
                            // Unused class
                            count++
                        } else {
                            // Used class
                            usedDirectClasses.add(declClass)
                        }
                    }

                    // Looking for used transitive dependencies
                    if (component.isTransitive
                        // Assume all these come from android.jar
                        && !declClass.startsWith("android.")
                        && usedClasses.contains(declClass)
                        // Not in the set of used direct dependencies
                        && !usedDirectClasses.contains(declClass)
                    ) {
                        classes.add(declClass)
                    }
                }

                if (count == component.classes.size
                    // Exclude modules that have inline usages
                    && component.hasNoInlineUsages()
                    // Exclude modules that have Android res usages
                    && component.hasNoAndroidResUsages()
                    // Exclude modules that have constant usages
                    && component.hasNoConstantUsages()
                ) {
                    unusedLibs.add(component.dependency)
                }

                if (classes.isNotEmpty()) {
                    usedTransitives.add(TransitiveComponent(component.dependency, classes))
                }
            }

        // Connect used-transitives to direct dependencies
        val unusedDepsWithTransitives = unusedLibs.mapNotNull { unusedLib ->
            root.dependencies.filterIsInstance<ResolvedDependencyResult>().find {
                unusedLib.identifier == it.selected.id.asString()
            }?.let {
                relate(it, UnusedDirectComponent(unusedLib, mutableSetOf()), usedTransitives.toSet())
            }
        }.toSet()

        // This is for printing to the console. A simplified view
        val completelyUnusedDeps = unusedDepsWithTransitives
            .filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency.identifier }
            .toSortedSet()

        return DependencyReport(
            unusedDepsWithTransitives,
            usedTransitives,
            completelyUnusedDeps
        )
    }

    private fun Component.hasNoInlineUsages(): Boolean {
        return usedInlineDependencies.none { it == dependency }
    }

    private fun Component.hasNoAndroidResUsages(): Boolean {
        return usedAndroidResDependencies?.none { it == dependency } ?: true
    }

    private fun Component.hasNoConstantUsages(): Boolean {
        return usedConstantDependencies.none { it == dependency }
    }

    /**
     * This recursive function maps used-transitives (undeclared dependencies, nevertheless used directly) to direct
     * dependencies (those actually declared "directly" in the build script).
     */
    private fun relate(
        resolvedDependency: ResolvedDependencyResult,
        unusedDep: UnusedDirectComponent,
        transitives: Set<TransitiveComponent>
    ): UnusedDirectComponent {
        resolvedDependency.selected.dependencies.filterIsInstance<ResolvedDependencyResult>().forEach {
            val identifier = it.selected.id.asString()
            val resolvedVersion = it.selected.id.resolvedVersion()

            if (transitives.map { trans -> trans.dependency.identifier }.contains(identifier)) {
                unusedDep.usedTransitiveDependencies.add(Dependency(identifier, resolvedVersion))
            }
            relate(it, unusedDep, transitives)
        }
        return unusedDep
    }

    internal class DependencyReport(
        val unusedDepsWithTransitives: Set<UnusedDirectComponent>,
        val usedTransitives: Set<TransitiveComponent>,
        val completelyUnusedDeps: Set<String>
    )
}

//private fun writeHtmlReport(
//    completelyUnusedDeps: Set<String>,
//    unusedDepsWithTransitives: Set<UnusedDirectComponent>,
//    usedTransitives: Set<TransitiveComponent>,
//    outputHtmlFile: File
//) {
//    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
//    document.create.html {
//        head { title("Misused Dependencies Report") }
//        body {
//            h1 { +"Completely unused direct dependencies" }
//            p {
//                em { +"You can remove these" }
//            }
//            table {
//                tr {
//                    td {}
//                    td { strong { +"Identifier" } }
//                }
//                completelyUnusedDeps.forEachIndexed { i, unusedDep ->
//                    tr {
//                        td { +"${i + 1}" }
//                        td { +unusedDep }
//                    }
//                }
//            }
//
//            h1 { +"Used transitive dependencies" }
//            p {
//                em { +"You should consider declaring these as direct dependencies" }
//            }
//            table {
//                tr {
//                    td {}
//                    td { strong { +"Identifier" } }
//                }
//                usedTransitives.forEachIndexed { i, trans ->
//                    tr {
//                        td { +"${i + 1}" }
//                        td {
//                            p { strong { +trans.dependency.identifier } }
//                            p {
//                                em { +"Used transitives" }
//                                ul {
//                                    trans.usedTransitiveClasses.forEach {
//                                        li { +it }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            h1 { +"Unused direct dependencies" }
//            p {
//                em { +"You only use the transitive dependencies of these dependencies. In some cases, you can remove use of these and just declare the transitives directly. In other cases, you should continue to declare these. This report is provided for informational purposes." }
//            }
//            table {
//                unusedDepsWithTransitives.forEachIndexed { i, unusedDep ->
//                    tr {
//                        // TODO is valign="bottom" supported?
//                        td { +"${i + 1}" }
//                        td {
//                            strong { +unusedDep.dependency.identifier }
//                            if (unusedDep.usedTransitiveDependencies.isNotEmpty()) {
//                                p {
//                                    em { +"Used transitives" }
//                                    ul {
//                                        unusedDep.usedTransitiveDependencies.forEach {
//                                            li { +it.identifier }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }.writeToFile(outputHtmlFile)
//}
