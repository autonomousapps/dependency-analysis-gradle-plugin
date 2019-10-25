@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.autonomousapps.internal.Artifact
import com.autonomousapps.internal.capitalize
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.TaskProvider
import java.io.File

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

    private fun Project.analyzeAndroidLibraryDependencies() {
        afterEvaluate {
            convention.findByType(LibraryExtension::class.java)!!.libraryVariants.all {
                val variantName = name.capitalize()

                val listClassesTask = registerClassAnalysisTasks(this)
                resolveCompileClasspathArtifacts(this)

                val dependencyReportTask =
                    tasks.register("dependenciesReport$variantName", DependencyReportTask::class.java) {
                        dependsOn(tasks.named("assemble$variantName"))

                        allArtifacts.set(layout.buildDirectory.file(PATH_ALL_ARTIFACTS))

                        output.set(layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS))
                        outputPretty.set(layout.buildDirectory.file(PATH_ALL_DECLARED_DEPS_PRETTY))
                    }

                tasks.register("misusedDependencies$variantName", DependencyMisuseTask::class.java) {
                    declaredDependencies.set(dependencyReportTask.flatMap { it.output })
                    usedClasses.set(listClassesTask.flatMap { it.output })
                    
                    outputUnusedDependencies.set(layout.buildDirectory.file(PATH_UNUSED_DIRECT_DEPS))
                    outputUsedTransitives.set(layout.buildDirectory.file(PATH_USED_TRANSITIVE_DEPS))
                }
            }
        }
    }

    // 1.
    // This produces a report that lists all of the used classes (FQCN) in the project
    private fun Project.registerClassAnalysisTasks(libraryVariant: LibraryVariant): TaskProvider<ClassAnalysisTask> {
        logger.quiet("lib variant: ${libraryVariant.name}")

        val name = libraryVariant.name.capitalize()

        // TODO this is unsafe. Task with this name not guaranteed to exist
        val bundleTask = tasks.named("bundleLibCompile$name", BundleLibraryClasses::class.java)

        // TODO this produces one task per variant, and so needs one output path per variant
        return tasks.register("listClassesFor$name", ClassAnalysisTask::class.java) {
            jar.set(bundleTask.flatMap { it.output })
            output.set(layout.buildDirectory.file(PATH_ALL_USED_CLASSES))
        }
    }

    // 2.
    // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type (library
    // vs project)
    // TODO currently sucks because:
    // a. Will run every time assembleDebug runs
    // b. Does IO during configuration
    private fun Project.resolveCompileClasspathArtifacts(libraryVariant: LibraryVariant) {
        configurations.all {
            // TODO need to reconsider how to get the correct configuration/classpath name
            // compileClasspath has the correct artifacts
            if (name == "debugCompileClasspath") {
                // This will gather all the resolved artifacts attached to the debugRuntimeClasspath INCLUDING transitives
                incoming.afterResolve {
                    val artifacts = artifactView {
                        attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
                    }.artifacts.artifacts
                        .map {
                            Artifact(
                                componentIdentifier = it.id.componentIdentifier,
                                file = it.file
                            )
                        }
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
}
