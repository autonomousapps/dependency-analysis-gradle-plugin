@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import java.io.File
import java.util.Locale

private const val PATH_ROOT = "class-analysis"
private const val PATH_ALL_USED_CLASSES = "$PATH_ROOT/all-used-classes.txt"
private const val FILE_ALL_ARTIFACTS = "all-artifacts.txt"
private const val FILE_ALL_ARTIFACTS_PRETTY = "all-artifacts-pretty.txt"
private const val PATH_ALL_ARTIFACTS = "$PATH_ROOT/$FILE_ALL_ARTIFACTS"
private const val PATH_ALL_DECLARED_DEPS = "$PATH_ROOT/all-declared-dependencies.txt"
private const val PATH_ALL_DECLARED_DEPS_PRETTY = "$PATH_ROOT/all-declared-dependencies-pretty.txt"
private const val PATH_UNUSED_DIRECT_DEPS = "$PATH_ROOT/unused-direct-dependencies.txt"
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

        val dependencyReportTask = tasks.register("dependenciesReport", DependencyReportTask::class.java)
        val misusedDependenciesTask = tasks.register("misusedDependencies", DependencyMisuseTask::class.java)
        afterEvaluate {
            dependencyReportTask.configure {
                dependsOn(tasks.named("assembleDebug"))

                allArtifacts.set(layout.buildDirectory.file(PATH_ALL_ARTIFACTS))
                output.set(layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS))
                outputPretty.set(layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS_PRETTY))
            }

            misusedDependenciesTask.configure {
                dependsOn(dependencyReportTask, tasks.named("listClassesForDebug"))

                declaredDependencies.set(layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS))
                usedClasses.set(layout.buildDirectory.file(PATH_ALL_USED_CLASSES))
                outputUnusedDependencies.set(layout.buildDirectory.file(PATH_UNUSED_DIRECT_DEPS))
                outputUsedTransitives.set(layout.buildDirectory.file(PATH_USED_TRANSITIVE_DEPS))
            }
        }
    }
}

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
