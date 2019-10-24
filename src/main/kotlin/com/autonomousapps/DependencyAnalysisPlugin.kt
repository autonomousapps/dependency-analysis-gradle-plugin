@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.autonomousapps.internal.ClassNameCollector
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.LibraryBinaryIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.objectweb.asm.ClassReader
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        pluginManager.withPlugin("com.android.application") {
            logger.debug("Adding Android tasks to ${project.name}")

        }
        pluginManager.withPlugin("com.android.library") {
            logger.debug("Adding Android tasks to ${project.name}")
            analyzeAndroidLibraryDependencies()
        }
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            logger.debug("Adding Kotlin tasks to ${project.name}")

        }
    }

    private fun Project.analyzeAndroidLibraryDependencies() {
        // 1.
        // This produces a report that lists all of the used classes (FQCN) in the project
        convention.findByType(LibraryExtension::class.java)!!.libraryVariants.all {
            logger.quiet("lib variant: $name")

            val name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1)

            // TODO this is unsafe. Task with this name not guaranteed to exist
            val bundleTask = tasks.named("bundleLibCompile$name", BundleLibraryClasses::class.java)
            tasks.register("listClassesFor$name", ClassAnalysisTask::class.java) {
                jar.set(bundleTask.flatMap { it.output })
                output.set(layout.buildDirectory.file("cloc/class-analysis/all-used-classes.txt"))
            }
        }

        // 2.
        // This is awful spike code that is intended to produce a report of all the declared classes (FQCN) depended-on by the project
        // These are obviously not necessarily actually used.
        var dependencyArtifacts: Set<Dep>? = null
        configurations.all {
            if (name == "debugCompileClasspath") {
                // This will gather all the resolved artifacts attached to the debugRuntimeClasspath INCLUDING transitives
                incoming.afterResolve {
                    //val a = deps.artifactView {
//                        it.attributes.attribute(Attribute.of("artifactType", String::class.java), "jar") // TODO this is producing paths to files which do not exist?
//                    }.artifacts.artifacts

                    val artifacts = artifactView {
                        attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
                    }.artifacts.artifacts

//                    logger.debug(("debugRuntimeClasspath artifacts\n${artifacts.joinToString(separator = "\n")}")

                    fun ComponentIdentifier.asString(): String {
                        return when (this) {
                            is ProjectComponentIdentifier -> projectPath
                            is ModuleComponentIdentifier -> moduleIdentifier.toString()
                            else -> throw GradleException("'This shouldn't happen'")
                        }
                    }

                    dependencyArtifacts = artifacts.map {
                        Dep(
                            // TODO just set the componentIdentifier and let the class work out the identifier and componentType
                            identifier = it.id.componentIdentifier.asString(),
                            componentType = ComponentType.of(it.id.componentIdentifier),
                            file = it.file
                        )
                    }.toSet()
//                    logger.debug(("DependencyWithFiles:\n${d.joinToString("\n")}")
                }
            }
        }

        // Get top-level dependencies. Necessary (?) to determine which deps are transitive.
        val confs = tasks.register("confs")
        val confs2 = tasks.register("confs2")
        val unused = tasks.register("unused")
        afterEvaluate {
            // Update dependencyArtifacts with `isTransitive` value
            confs.configure {
                dependsOn(tasks.named("assembleDebug"))

                doLast {
                    logger.debug("===Traversing dependencies===")

                    val conf = configurations.getByName("debugRuntimeClasspath")
                    val result: ResolutionResult = conf.incoming.resolutionResult
                    val root: ResolvedComponentResult = result.root
                    val dependents: Set<ResolvedDependencyResult> = root.dependents
                    val dependencies: Set<DependencyResult> = root.dependencies
                    val allDependencies: Set<DependencyResult> = result.allDependencies

                    val deps = traverseDependencies(0, dependencies)

                    dependencyArtifacts!!.forEach { dep ->
                        dep.apply {
                            isTransitive = !deps.any { it.identifier == dep.identifier }
                            file = dep.file
                        }
                    }
//                  logger.debug(("DepWithFile2\n${d!!.joinToString("\n")}")
                }
            }
            // end confs.configure {}

            // Generate list of all classes declared by all dependencyArtifacts
            confs2.configure {
                    dependsOn(confs)

                    doLast {
                        val output =
                            layout.buildDirectory.file("cloc/class-analysis/all-declared-dependencies.txt").get().asFile
                        output.delete()

                        // TODO remove
                        val outputPretty =
                            layout.buildDirectory.file("cloc/class-analysis/all-declared-dependencies-pretty.txt").get()
                                .asFile
                        outputPretty.delete()

                        val libraries = dependencyArtifacts!!.filter {
                            if (!it.file!!.exists()) {
                                logger.error("File doesn't exist for dep $it")
                            }
                            it.file!!.exists()
                        }.map { dep ->
                            val z = ZipFile(dep.file)

                            val classes = z.entries().toList()
                                .filterNot { it.isDirectory }
                                .filter { it.name.endsWith(".class") }
                                .map { classEntry ->
                                    val classNameCollector = ClassNameCollector(logger) // was ClassPrinter
                                    val reader = ClassReader(z.getInputStream(classEntry).readBytes())
                                    reader.accept(classNameCollector, 0)
                                    classNameCollector
                                }
                                .mapNotNull { it.className }
                                .filterNot {
                                    // Filter out `java` packages, but not `javax`
                                    it.startsWith("java/")
                                }
                                .toSet()
                                .map { it.replace("/", ".") }
                                .sorted()

                            Library(dep.identifier, dep.isTransitive!!, classes)
                        }.sorted()

                        output.writeText(libraries.toJson())
                        outputPretty.writeText(libraries.toPrettyString())
                    }
            }

            unused.configure {
                dependsOn(confs2, tasks.named("listClassesForDebug"))

                doLast {
                    // Inputs
                    val decl =
                        layout.buildDirectory.file("cloc/class-analysis/all-declared-dependencies.txt").get().asFile
                    val used = layout.buildDirectory.file("cloc/class-analysis/all-used-classes.txt").get().asFile

                    val outputUnused =
                        layout.buildDirectory.file("cloc/class-analysis/used-transitive-dependencies.txt").get().asFile
                    outputUnused.delete()

                    val outputUsedTransitives =
                        layout.buildDirectory.file("cloc/class-analysis/used-transitive-dependencies.txt").get().asFile
                    outputUsedTransitives.delete()

                    val declaredLibs = decl.readText().fromJsonList<Library>()
                    val usedClasses = used.readLines()

                    // Algorithm
                    val unusedLibs = mutableListOf<String>()
                    val usedTransitives = mutableListOf<TransitiveDependency>()
                    val usedDirectClasses = mutableListOf<String>()
                    declaredLibs
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

                    outputUnused.writeText(unusedLibs.joinToString("\n"))
                    logger.quiet("Unused dependencies:\n${unusedLibs.joinToString("\n")}\n")

                    // TODO known issues:
                    // 1. org.jetbrains.kotlin:kotlin-stdlib should be excluded TODO or maybe not?
                    // TODO 2. generated code might used transitives (such as dagger.android using vanilla dagger; and org.jetbrains:annotations).
                    // 3. Some deps might be direct AND transitive, and I don't currently de-dup this. See nl.qbusict:cupboard, which references Context
                    // 4. Some deps come from android.jar, and should be excluded
                    outputUsedTransitives.writeText(usedTransitives.toJson())
                    logger.quiet("Used transitive dependencies:\n${usedTransitives.toPrettyString()}")
                }
            }
        }
    }
}

private fun traverseDependencies(level: Int, results: Set<DependencyResult>): Set<Dep> {

    fun calculateIndentation(level: Int) = "     ".repeat(level)

    val deps = mutableSetOf<Dep>()

    results.forEach { result ->
        if (result is ResolvedDependencyResult) {
            val componentResult: ResolvedComponentResult = result.selected
            // LibraryBinaryIdentifier, ModuleComponentIdentifier, ProjectComponentIdentifier
            val componentIdentifier: ComponentIdentifier = componentResult.id
            val compType = when (componentIdentifier) {
                is LibraryBinaryIdentifier -> "lib"
                is ModuleComponentIdentifier -> "mod"
                is ProjectComponentIdentifier -> "pro"
                else -> throw GradleException("'This should never happen'")
            }

            val node =
                "${calculateIndentation(level)}- ($compType) ${componentIdentifier.displayName} (${componentResult.selectionReason})"
//            println(node)
//            traverseDependencies(level + 1, componentResult.dependencies)

            if (componentIdentifier is ProjectComponentIdentifier) {
                deps.add(Dep(identifier = componentIdentifier.projectPath, componentType = ComponentType.PROJECT))
            } else if (componentIdentifier is ModuleComponentIdentifier) {
                deps.add(
                    Dep(
                        identifier = componentIdentifier.moduleIdentifier.toString(),
                        componentType = ComponentType.LIBRARY
                    )
                )
            }
        } else if (result is UnresolvedDependencyResult) {
            val componentSelector: ComponentSelector = result.attempted
            val node = "${calculateIndentation(level)}- ${componentSelector.displayName} (failed)"
            println(node)
        }
    }

    return deps
}

data class Dep(
    val identifier: String,
    val componentType: ComponentType,
    var isTransitive: Boolean? = null,
    var file: File? = null
) {
    data class Builder(
        private var identifier: String? = null,
        private var componentType: ComponentType? = null,
        private var isTransitive: Boolean? = null,
        private var file: File? = null
    ) {
        fun identifier(identifier: String) = apply { this.identifier = identifier }
        fun componentType(componentType: ComponentType) = apply { this.componentType = componentType }
        fun isTransitive(isTransitive: Boolean) = apply { this.isTransitive = isTransitive }
        fun file(file: File) = apply { this.file = file }
        fun build() = Dep(identifier!!, componentType!!, isTransitive!!, file!!)
    }
}

enum class ComponentType {
    LIBRARY, PROJECT;

    companion object {
        fun of(componentIdentifier: ComponentIdentifier) = when (componentIdentifier) {
            is ModuleComponentIdentifier -> LIBRARY
            is ProjectComponentIdentifier -> PROJECT
            else -> throw GradleException("'This shouldn't happen'")
        }
    }
}

data class Library(
    val identifier: String,
    val isTransitive: Boolean,
    val classes: List<String> // TODO Set
) : Comparable<Library> {

    override fun compareTo(other: Library): Int {
        return identifier.compareTo(other.identifier)
    }
}

data class TransitiveDependency(
    val identifier: String,
    val usedTransitiveClasses: List<String> // TODO Set
)
