@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.android.build.gradle.tasks.AndroidJavaCompile
import com.autonomousapps.internal.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        pluginManager.withPlugin(ANDROID_APP_PLUGIN) {
            logger.debug("Adding Android tasks to ${project.name}")
            analyzeAndroidApplicationDependencies()
        }
        pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN) {
            logger.debug("Adding Android tasks to ${project.name}")
            analyzeAndroidLibraryDependencies()
        }
        pluginManager.withPlugin(KOTLIN_JVM_PLUGIN) {
            logger.debug("Adding Kotlin tasks to ${project.name}")
            analyzeKotlinJvmDependencies()
        }
    }

    private fun Project.analyzeAndroidApplicationDependencies() {
        // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks.
        afterEvaluate {
            the<AppExtension>().applicationVariants.all {
                val androidClassAnalyzer = AppClassAnalyzer(this@analyzeAndroidApplicationDependencies, name)
                analyzeAndroidDependencies(androidClassAnalyzer)
            }
        }
    }

    private fun Project.analyzeAndroidLibraryDependencies() {
        the<LibraryExtension>().libraryVariants.all {
            val androidClassAnalyzer = LibClassAnalyzer(this@analyzeAndroidLibraryDependencies, name)
            analyzeAndroidDependencies(androidClassAnalyzer)//name)
        }
    }

    private fun Project.analyzeKotlinJvmDependencies() {
        // TODO
    }

    private fun <T : ClassAnalysisTask> Project.analyzeAndroidDependencies(androidClassAnalyzer: AndroidClassAnalyzer<T>) {
        // Convert `flavorDebug` to `FlavorDebug`
        val variantName = androidClassAnalyzer.variantName
        val variantTaskName = androidClassAnalyzer.variantNameCapitalized

        val analyzeClassesTask = androidClassAnalyzer.registerClassAnalysisTask()

        // 2.
        // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type
        // (library vs project)
        val artifactsReportTask = tasks.register("artifactsReport$variantTaskName", ArtifactsAnalysisTask::class.java) {
            //            dependsOn(androidClassAnalyzer.artifactsTaskDependency) // TODO if this truly isn't needed, might be able to remove the property from the interface.

            val artifacts = configurations["${variantName}CompileClasspath"].incoming.artifactView {
                attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
            }.artifacts

            artifactFiles = artifacts.artifactFiles
            resolvedArtifacts = artifacts.artifacts

            output.set(layout.buildDirectory.file(getArtifactsPath(variantName)))
            outputPretty.set(layout.buildDirectory.file(getArtifactsPrettyPath(variantName)))
        }

        val dependencyReportTask =
            tasks.register("dependenciesReport$variantTaskName", DependencyReportTask::class.java) {
                dependsOn(artifactsReportTask)

                allArtifacts.set(artifactsReportTask.flatMap { it.output })

                output.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantName)))
                outputPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantName)))
            }

        tasks.register("misusedDependencies$variantTaskName", DependencyMisuseTask::class.java) {
            declaredDependencies.set(dependencyReportTask.flatMap { it.output })
            usedClasses.set(analyzeClassesTask.flatMap { it.output })

            outputUnusedDependencies.set(
                layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))
            )
            outputUsedTransitives.set(
                layout.buildDirectory.file(getUsedTransitiveDependenciesPath(variantName))
            )
        }
    }

    private interface AndroidClassAnalyzer<T : ClassAnalysisTask> {
        val variantName: String
        val variantNameCapitalized: String
        val artifactsTaskDependency: String

        // 1.
        // This produces a report that lists all of the used classes (FQCN) in the project
        fun registerClassAnalysisTask(): TaskProvider<out T>
    }

    private class LibClassAnalyzer(
        private val project: Project,
        override val variantName: String
    ) : AndroidClassAnalyzer<JarAnalysisTask> {

        override val variantNameCapitalized: String = variantName.capitalize()

        // TODO this is unsafe. Task with this name not guaranteed to exist. Definitely known to exist in AGP 3.5.
        override val artifactsTaskDependency: String = "bundleLibCompile$variantNameCapitalized"

        override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
            val bundleTask = project.tasks.named(artifactsTaskDependency, BundleLibraryClasses::class.java)

            return project.tasks.register("analyzeClassUsage$variantNameCapitalized", JarAnalysisTask::class.java) {
                jar.set(bundleTask.flatMap { it.output })
                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }
    }

    private class AppClassAnalyzer(
        private val project: Project,
        override val variantName: String
    ) : AndroidClassAnalyzer<ClassListAnalysisTask> {

        override val variantNameCapitalized: String = variantName.capitalize()
        override val artifactsTaskDependency: String = "assemble$variantNameCapitalized"

        override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
            // TODO this is unsafe. Task with these names not guaranteed to exist. Definitely known to exist in AGP 3.5 & Kotlin 1.3.50.
            val kotlinCompileTask = project.tasks.named("compile${variantNameCapitalized}Kotlin", KotlinCompile::class.java)
            val javaCompileTask = project.tasks.named("compile${variantNameCapitalized}JavaWithJavac", AndroidJavaCompile::class.java)

            return project.tasks.register("analyzeClassUsage$variantNameCapitalized", ClassListAnalysisTask::class.java) {
                kotlinClasses.plus(kotlinCompileTask.get().outputs.files.asFileTree)
                javaClasses.set(javaCompileTask.flatMap { it.outputDirectory })

                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }
    }
}

private fun getVariantDirectory(variantName: String) = "dependency-analysis/$variantName"

private fun getArtifactsPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts.txt"

private fun getArtifactsPrettyPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts-pretty.txt"

private fun getAllUsedClassesPath(variantName: String) = "${getVariantDirectory(variantName)}/all-used-classes.txt"

private fun getAllDeclaredDepsPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies.txt"

private fun getAllDeclaredDepsPrettyPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies-pretty.txt"

private fun getUnusedDirectDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/unused-direct-dependencies.txt"

private fun getUsedTransitiveDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/used-transitive-dependencies.txt"
