@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.tasks.BundleLibraryClasses
import com.autonomousapps.internal.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val JAVA_LIBRARY_PLUGIN = "java-library"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = project.run {
        pluginManager.withPlugin(ANDROID_APP_PLUGIN) {
            logger.debug("Adding Android tasks to ${project.path}")
            analyzeAndroidApplicationDependencies()
        }
        pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN) {
            logger.debug("Adding Android tasks to ${project.path}")
            analyzeAndroidLibraryDependencies()
        }
        pluginManager.withPlugin(JAVA_LIBRARY_PLUGIN) {
            logger.quiet("Adding JVM tasks to ${project.path}")
            analyzeJavaLibraryDependencies()
        }
    }

    private fun Project.analyzeAndroidApplicationDependencies() {
        // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks.
        afterEvaluate {
            the<AppExtension>().applicationVariants.all {
                val androidClassAnalyzer = AppClassAnalyzer(this@analyzeAndroidApplicationDependencies, this)
                analyzeAndroidDependencies(androidClassAnalyzer)
            }
        }
    }

    private fun Project.analyzeAndroidLibraryDependencies() {
        the<LibraryExtension>().libraryVariants.all {
            val androidClassAnalyzer = LibClassAnalyzer(this@analyzeAndroidLibraryDependencies, this)
            analyzeAndroidDependencies(androidClassAnalyzer)
        }
    }

    private fun Project.analyzeJavaLibraryDependencies() {
        the<JavaPluginConvention>().sourceSets.forEach { sourceSet ->
            try {
                val javaModuleClassAnalyzer = JavaModuleClassAnalyzer(this, sourceSet)
                analyzeAndroidDependencies(javaModuleClassAnalyzer)
            } catch (e: UnknownTaskException) {
                logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
            }
        }
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
            val artifactCollection = configurations[androidClassAnalyzer.compileConfigurationName].incoming.artifactView {
                attributes.attribute(Attribute.of("artifactType", String::class.java), androidClassAnalyzer.attributeValue)
            }.artifacts

            artifactFiles = artifactCollection.artifactFiles
            artifacts = artifactCollection

            output.set(layout.buildDirectory.file(getArtifactsPath(variantName)))
            outputPretty.set(layout.buildDirectory.file(getArtifactsPrettyPath(variantName)))
        }

        val dependencyReportTask =
            tasks.register("dependenciesReport$variantTaskName", DependencyReportTask::class.java) {
                dependsOn(artifactsReportTask) // TODO redundant?

                configurationName.set(androidClassAnalyzer.runtimeConfigurationName)
                allArtifacts.set(artifactsReportTask.flatMap { it.output })

                output.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantName)))
                outputPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantName)))
            }

        tasks.register("misusedDependencies$variantTaskName", DependencyMisuseTask::class.java) {
            declaredDependencies.set(dependencyReportTask.flatMap { it.output })
            usedClasses.set(analyzeClassesTask.flatMap { it.output })
            configurationName.set(androidClassAnalyzer.runtimeConfigurationName)

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
        val compileConfigurationName: String
        val runtimeConfigurationName: String
        val attributeValue: String

        // 1.
        // This produces a report that lists all of the used classes (FQCN) in the project
        fun registerClassAnalysisTask(): TaskProvider<out T>
    }

    private class LibClassAnalyzer(
        private val project: Project,
        private val variant: BaseVariant
    ) : AndroidClassAnalyzer<JarAnalysisTask> {

        override val variantName: String = variant.name
        override val variantNameCapitalized: String = variantName.capitalize()
        override val compileConfigurationName = "${variantName}CompileClasspath"
        override val runtimeConfigurationName = "${variantName}RuntimeClasspath"
        override val attributeValue = "android-classes"

        override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
            // Known to exist in AGP 3.5 and 3.6
            val bundleTask = project.tasks.named("bundleLibCompile$variantNameCapitalized", BundleLibraryClasses::class.java)

            // Best guess as to path to kapt-generated Java stubs
            val kaptStubs = project.layout.buildDirectory.asFileTree.matching {
                include("**/kapt*/**/${variantName}/**/*.java")
            }

            return project.tasks.register("analyzeClassUsage$variantNameCapitalized", JarAnalysisTask::class.java) {
                jar.set(bundleTask.flatMap { it.output })
                kaptJavaStubs.from(kaptStubs)
                layouts(variant.sourceSets.flatMap { it.resDirectories })

                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }
    }

    private class AppClassAnalyzer(
        private val project: Project,
        private val variant: BaseVariant
    ) : AndroidClassAnalyzer<ClassListAnalysisTask> {

        override val variantName: String = variant.name
        override val variantNameCapitalized: String = variantName.capitalize()
        override val compileConfigurationName = "${variantName}CompileClasspath"
        override val runtimeConfigurationName = "${variantName}RuntimeClasspath"
        override val attributeValue = "android-classes"

        override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
            // Known to exist in Kotlin 1.3.50.
            val kotlinCompileTask = project.tasks.named("compile${variantNameCapitalized}Kotlin", KotlinCompile::class.java)
            // Known to exist in AGP 3.5 and 3.6, albeit with different backing classes (AndroidJavaCompile and JavaCompile)
            val javaCompileTask = project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")

            // Best guess as to path to kapt-generated Java stubs
            val kaptStubs = project.layout.buildDirectory.asFileTree.matching {
                include("**/kapt*/**/${variantName}/**/*.java")
            }

            return project.tasks.register("analyzeClassUsage$variantNameCapitalized", ClassListAnalysisTask::class.java) {
                kotlinClasses.from(kotlinCompileTask.get().outputs.files.asFileTree)
                javaClasses.from(javaCompileTask.get().outputs.files.asFileTree)
                kaptJavaStubs.from(kaptStubs)
                layouts(variant.sourceSets.flatMap { it.resDirectories })

                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }
    }

    private class JavaModuleClassAnalyzer(
        private val project: Project,
        private val sourceSet: SourceSet
    ) : AndroidClassAnalyzer<JarAnalysisTask> {

        override val variantName = sourceSet.name
        override val variantNameCapitalized = variantName.capitalize()
        // Yes, these two are the same for this case
        override val compileConfigurationName = "compileClasspath"
        override val runtimeConfigurationName = "compileClasspath"
        override val attributeValue = "jar"

        override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
            val jarTask = project.tasks.named(sourceSet.jarTaskName, Jar::class.java)
            // Best guess as to path to kapt-generated Java stubs
            val kaptStubs = project.layout.buildDirectory.asFileTree.matching {
                include("**/kapt*/**/${variantName}/**/*.java")
            }

            return project.tasks.register("analyzeClassUsage$variantNameCapitalized", JarAnalysisTask::class.java) {
                jar.set(jarTask.flatMap { it.archiveFile })
                kaptJavaStubs.from(kaptStubs)
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
