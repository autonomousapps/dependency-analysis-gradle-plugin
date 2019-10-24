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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.objectweb.asm.ClassReader
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile

private const val PATH_ROOT = "class-analysis"
private const val PATH_ALL_USED_CLASSES = "$PATH_ROOT/all-used-classes.txt"
private const val FILE_ALL_ARTIFACTS = "all-artifacts.txt"
private const val FILE_ALL_ARTIFACTS_PRETTY = "all-artifacts-pretty.txt"
private const val PATH_ALL_ARTIFACTS = "$PATH_ROOT/$FILE_ALL_ARTIFACTS"
private const val PATH_ALL_DECLARED_DEPS = "$PATH_ROOT/all-declared-dependencies.txt"
private const val PATH_ALL_DECLARED_DEPS_PRETTY = "$PATH_ROOT/all-declared-dependencies-pretty.txt"
private const val PATH_USED_TRANSITIVE_DEPS = "$PATH_ROOT/used-transitive-dependencies.txt"

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

    // 1.
    // This produces a report that lists all of the used classes (FQCN) in the project
    private fun Project.registerClassAnalysisTasks() {
        convention.findByType(LibraryExtension::class.java)!!.libraryVariants.all {
            logger.quiet("lib variant: $name")

            val name = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1)

            // TODO this is unsafe. Task with this name not guaranteed to exist
            val bundleTask = tasks.named("bundleLibCompile$name", BundleLibraryClasses::class.java)
            tasks.register("listClassesFor$name", ClassAnalysisTask::class.java) {
                jar.set(bundleTask.flatMap { it.output })
                output.set(layout.buildDirectory.file(PATH_ALL_USED_CLASSES))
            }
        }
    }

    // 2.
    // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type (library
    // vs project)
    // TODO currently sucks because:
    // 1. Will run every time assembleDebug runs
    // 2. Does IO during configuration
    private fun Project.resolveCompileClasspathArtifacts() {
        configurations.all {
            // TODO need to reconsider how to get the correct configuration/classpath name
            // compileClasspath has the correct artifacts
            if (name == "debugCompileClasspath") {
                // This will gather all the resolved artifacts attached to the debugRuntimeClasspath INCLUDING transitives
                incoming.afterResolve {
                    val artifacts = artifactView {
                        attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
                    }.artifacts.artifacts
                        .map { Artifact(componentIdentifier = it.id.componentIdentifier, file = it.file) }
                        .toSet()

                    val artifactsFileRoot = File(project.file(buildDir), PATH_ROOT).apply { mkdirs() }
                    val artifactsFile = File(artifactsFileRoot, FILE_ALL_ARTIFACTS).also {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                    val artifactsFilePretty = File(artifactsFileRoot, FILE_ALL_ARTIFACTS_PRETTY).also {
                        if (it.exists()) {
                            it.delete()
                        }
                    }

                    artifactsFile.writeText(artifacts.toJson())
                    artifactsFilePretty.writeText(artifacts.toPrettyString())
                }
            }
        }
    }

    private fun Project.analyzeAndroidLibraryDependencies() {
        registerClassAnalysisTasks()
        resolveCompileClasspathArtifacts()
        

        // Get top-level dependencies. Necessary (?) to determine which deps are transitive.
        val confs = tasks.register("confs")
        val unused = tasks.register("unused")
        afterEvaluate {
            // 3. Update dependencyArtifacts with `isTransitive` value
            confs.configure {
                dependsOn(tasks.named("assembleDebug"))

                doLast {
                    // Step 1
                    val artifactsFile = layout.buildDirectory.file(PATH_ALL_ARTIFACTS).get().asFile
                    val artifacts = artifactsFile.readText().fromJsonList<Artifact>()

                    // runtime classpath will give me only the direct dependencies
                    val conf = configurations.getByName("debugRuntimeClasspath")
                    val result: ResolutionResult = conf.incoming.resolutionResult
                    val root: ResolvedComponentResult = result.root
                    val dependents: Set<ResolvedDependencyResult> = root.dependents
                    val dependencies: Set<DependencyResult> = root.dependencies
                    val allDependencies: Set<DependencyResult> = result.allDependencies

                    val deps = traverseDependencies(dependencies)

                    artifacts.forEach { dep ->
                        dep.apply {
                            isTransitive = !deps.any { it.identifier == dep.identifier }
                        }
                    }

                    // Step 2
                    // Generate list of all classes declared by all dependencyArtifacts
                    val output = layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS).get().asFile
                    output.delete()

                    // TODO remove
                    val outputPretty = layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS_PRETTY).get().asFile
                    outputPretty.delete()

                    val libraries = artifacts.filter {
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
                dependsOn(confs, tasks.named("listClassesForDebug"))

                doLast {
                    // Inputs
                    val decl =
                        layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS).get().asFile
                    val used = layout.buildDirectory.file(PATH_ALL_USED_CLASSES).get().asFile

                    val outputUnused = layout.buildDirectory.file(PATH_USED_TRANSITIVE_DEPS).get().asFile
                    outputUnused.delete()

                    val outputUsedTransitives = layout.buildDirectory.file(PATH_USED_TRANSITIVE_DEPS).get().asFile
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

private fun traverseDependencies(results: Set<DependencyResult>): Set<Artifact> = results
    .filterIsInstance<ResolvedDependencyResult>()
    .map { result ->
        val componentResult = result.selected

        when (val componentIdentifier = componentResult.id) {
            is ProjectComponentIdentifier -> Artifact(componentIdentifier)
            is ModuleComponentIdentifier -> Artifact(componentIdentifier)
            else -> throw GradleException("Unexpected ComponentIdentifier type: ${componentIdentifier.javaClass.simpleName}")
        }
    }.toSet()

data class Artifact(
    val identifier: String,
    val componentType: ComponentType,
    var isTransitive: Boolean? = null,
    var file: File? = null
) {

    constructor(componentIdentifier: ComponentIdentifier, file: File? = null) : this(
        identifier = componentIdentifier.asString(),
        componentType = ComponentType.of(componentIdentifier),
        file = file
    )
}

private fun ComponentIdentifier.asString(): String {
    return when (this) {
        is ProjectComponentIdentifier -> projectPath
        is ModuleComponentIdentifier -> moduleIdentifier.toString()
        else -> throw GradleException("Cannot identify ComponentIdentifier subtype. Was ${javaClass.simpleName}")
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
