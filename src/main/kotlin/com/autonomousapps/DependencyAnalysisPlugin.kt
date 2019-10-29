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

private const val FILENAME_ALL_ARTIFACTS = "all-artifacts.txt"
private const val FILENAME_ALL_ARTIFACTS_PRETTY = "all-artifacts-pretty.txt"

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
        convention.findByType(LibraryExtension::class.java)!!.libraryVariants.all {
            val variantPathName = name
            val variantTaskName = name.capitalize()

            val listClassesTask = registerClassAnalysisTasks(this)
            resolveCompileClasspathArtifacts(this)

            val dependencyReportTask =
                tasks.register("dependenciesReport$variantTaskName", DependencyReportTask::class.java) {

                    dependsOn(tasks.named("assemble$variantTaskName"))

                    allArtifacts.set(layout.buildDirectory.file(getAllArtifactsPath(variantPathName)))

                    output.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantPathName)))
                    outputPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantPathName)))
                }

            tasks.register("misusedDependencies$variantTaskName", DependencyMisuseTask::class.java) {
                declaredDependencies.set(dependencyReportTask.flatMap { it.output })
                usedClasses.set(listClassesTask.flatMap { it.output })

                outputUnusedDependencies.set(
                    layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantPathName))
                )
                outputUsedTransitives.set(
                    layout.buildDirectory.file(getUsedTransitiveDependenciesPath(variantPathName))
                )
            }
        }
    }

    // 1.
    // This produces a report that lists all of the used classes (FQCN) in the project
    private fun Project.registerClassAnalysisTasks(libraryVariant: LibraryVariant): TaskProvider<ClassAnalysisTask> {
        val variantPathName = libraryVariant.name
        val variantTaskName = libraryVariant.name.capitalize()

        // TODO this is unsafe. Task with this name not guaranteed to exist. Definitely known to exist in AGP 3.5.
        val bundleTask = tasks.named("bundleLibCompile$variantTaskName", BundleLibraryClasses::class.java)

        return tasks.register("listClassesFor$variantTaskName", ClassAnalysisTask::class.java) {
            jar.set(bundleTask.flatMap { it.output })
            output.set(layout.buildDirectory.file(getAllUsedClassesPath(variantPathName)))
        }
    }

    // 2.
    // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type (library
    // vs project)
    // TODO currently sucks because:
    // a. Will run every time assembleDebug runs (I think because Kotlin causes eager realization of all AGP tasks)
    // b. Does IO during configuration (only because of a.)
    // c. Trying to run with a `clean` will fail horribly. The clean will remove the output generated during the config phase.
    private fun Project.resolveCompileClasspathArtifacts(libraryVariant: LibraryVariant) {
        // TODO I don't want to do this inside the libVariants loop. Or maybe I must?
        configurations.all {
            // compileClasspath has the correct artifacts
            if (name == "${libraryVariant.name}CompileClasspath") {
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

                    // TODO: instead of writing files, use serializable classes and use them as in-memory inputs directly.
                    val artifactsFileRoot = File(
                        project.file(buildDir),
                        getVariantDirectory(libraryVariant.name)
                    ).apply { mkdirs() }
                    val artifactsFile = File(artifactsFileRoot, FILENAME_ALL_ARTIFACTS)
                    val artifactsFilePretty = File(artifactsFileRoot, FILENAME_ALL_ARTIFACTS_PRETTY)

                    artifactsFile.writeText(artifacts.toJson())
                    artifactsFilePretty.writeText(artifacts.toPrettyString())
                }
            }
        }
    }

    private fun getVariantDirectory(variantName: String) = "dependency-analysis/$variantName"

    private fun getAllUsedClassesPath(variantName: String) = "${getVariantDirectory(variantName)}/all-used-classes.txt"

    private fun getAllArtifactsPath(variantName: String) = "${getVariantDirectory(variantName)}/$FILENAME_ALL_ARTIFACTS"

    private fun getAllDeclaredDepsPath(variantName: String) =
        "${getVariantDirectory(variantName)}/all-declared-dependencies.txt"

    private fun getAllDeclaredDepsPrettyPath(variantName: String) =
        "${getVariantDirectory(variantName)}/all-declared-dependencies-pretty.txt"

    private fun getUnusedDirectDependenciesPath(variantName: String) =
        "${getVariantDirectory(variantName)}/unused-direct-dependencies.txt"

    private fun getUsedTransitiveDependenciesPath(variantName: String) =
        "${getVariantDirectory(variantName)}/used-transitive-dependencies.txt"
}
