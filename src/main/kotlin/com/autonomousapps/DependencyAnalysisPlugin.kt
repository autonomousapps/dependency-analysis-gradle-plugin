@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
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
import org.gradle.kotlin.dsl.*
import java.util.concurrent.atomic.AtomicBoolean

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val JAVA_LIBRARY_PLUGIN = "java-library"

private const val EXTENSION_NAME = "dependencyAnalysis"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

    private fun Project.getExtension(): DependencyAnalysisExtension? =
        rootProject.extensions.findByType()

    private val artifactAdded = AtomicBoolean(false)

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
            logger.debug("Adding JVM tasks to ${project.path}")
            // for Java library projects, use a different convention
            getExtension()?.theVariants?.convention(listOf(JAVA_LIB_SOURCE_SET_DEFAULT))
            analyzeJavaLibraryDependencies()
        }

        if (this == rootProject) {
            logger.debug("Adding root project tasks")

            extensions.create<DependencyAnalysisExtension>(EXTENSION_NAME, objects)
            addAggregatingTasks()
            subprojects {
                apply(plugin = "com.autonomousapps.dependency-analysis")
            }
        }
    }

    private fun Project.analyzeAndroidApplicationDependencies() {
        // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks. This is due to use of the
        // pluginManager.withPlugin API. Currently configuring the com.android.application plugin, not any Kotlin
        // plugin. I do not know how to wait for both plugins to be ready.
        afterEvaluate {
            the<AppExtension>().applicationVariants.all {
                val androidClassAnalyzer = AndroidAppAnalyzer(this@analyzeAndroidApplicationDependencies, this)
                analyzeDependencies(androidClassAnalyzer)
            }
        }
    }

    private fun Project.analyzeAndroidLibraryDependencies() {
        the<LibraryExtension>().libraryVariants.all {
            val androidClassAnalyzer = AndroidLibAnalyzer(this@analyzeAndroidLibraryDependencies, this)
            analyzeDependencies(androidClassAnalyzer)
        }
    }

    private fun Project.analyzeJavaLibraryDependencies() {
        the<JavaPluginConvention>().sourceSets
            .filterNot { it.name == "test" }
            .forEach { sourceSet ->
                try {
                    val javaModuleClassAnalyzer = JavaLibAnalyzer(this, sourceSet)
                    analyzeDependencies(javaModuleClassAnalyzer)
                } catch (e: UnknownTaskException) {
                    logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
                }
            }
    }

    /**
     * Tasks are registered here. This function is called in a loop, once for each Android variant or Java source set.
     */
    private fun <T : ClassAnalysisTask> Project.analyzeDependencies(dependencyAnalyzer: DependencyAnalyzer<T>) {
        val variantName = dependencyAnalyzer.variantName
        val variantTaskName = dependencyAnalyzer.variantNameCapitalized

        val analyzeClassesTask = dependencyAnalyzer.registerClassAnalysisTask()

        // 2.
        // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type
        // (library vs project)
        val artifactsReportTask = tasks.register<ArtifactsAnalysisTask>("artifactsReport$variantTaskName") {
            val artifactCollection =
                configurations[dependencyAnalyzer.compileConfigurationName].incoming.artifactView {
                    attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                }.artifacts

            artifactFiles = artifactCollection.artifactFiles
            artifacts = artifactCollection

            output.set(layout.buildDirectory.file(getArtifactsPath(variantName)))
            outputPretty.set(layout.buildDirectory.file(getArtifactsPrettyPath(variantName)))
        }

        val dependencyReportTask =
            tasks.register<DependencyReportTask>("dependenciesReport$variantTaskName") {
                artifactFiles =
                    configurations.getByName(dependencyAnalyzer.runtimeConfigurationName).incoming.artifactView {
                        attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                    }.artifacts.artifactFiles
                configurationName.set(dependencyAnalyzer.runtimeConfigurationName)
                allArtifacts.set(artifactsReportTask.flatMap { it.output })

                output.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantName)))
                outputPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantName)))
            }

        val misusedDependenciesTask = tasks.register<DependencyMisuseTask>("misusedDependencies$variantTaskName") {
            artifactFiles =
                configurations.getByName(dependencyAnalyzer.runtimeConfigurationName).incoming.artifactView {
                    attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                }.artifacts.artifactFiles
            configurationName.set(dependencyAnalyzer.runtimeConfigurationName)
            declaredDependencies.set(dependencyReportTask.flatMap { it.output })
            usedClasses.set(analyzeClassesTask.flatMap { it.output })

            outputUnusedDependencies.set(
                layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))
            )
            outputUsedTransitives.set(
                layout.buildDirectory.file(getUsedTransitiveDependenciesPath(variantName))
            )
            outputHtml.set(
                layout.buildDirectory.file(getMisusedDependenciesHtmlPath(variantName))
            )
        }

        val abiAnalysisTask = dependencyAnalyzer.registerAbiAnalysisTask(dependencyReportTask)

        // We must only do this once per project
        if (shouldAddArtifact(variantName)) {
            artifactAdded.set(true)

            // Configure misused dependencies aggregate tasks
            val dependencyReports = configurations.create("dependencyReport") {
                isCanBeResolved = false
            }
            artifacts {
                add(dependencyReports.name, layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))) {
                    builtBy(misusedDependenciesTask)
                }
            }
            rootProject.dependencies {
                add(dependencyReports.name, project(this@analyzeDependencies.path, dependencyReports.name))
            }

            // Configure ABI analysis aggregate task
            abiAnalysisTask?.let {
                val abiReport = configurations.create("abiReport") {
                    isCanBeResolved = false
                }
                artifacts {
                    add(abiReport.name, layout.buildDirectory.file(getAbiAnalysisPath(variantName))) {
                        builtBy(abiAnalysisTask)
                    }
                }
                rootProject.dependencies {
                    add(abiReport.name, project(this@analyzeDependencies.path, abiReport.name))
                }
            }
        }
    }

    private fun Project.shouldAddArtifact(variantName: String): Boolean {
        if (artifactAdded.get()) {
            return false
        }

        return getExtension()?.getFallbacks()?.contains(variantName) == true
    }

    private fun Project.addAggregatingTasks() {
        val dependencyReports = configurations.create("dependencyReport") {
            isCanBeConsumed = false
        }
        val abiReportsConf = configurations.create("abiReport") {
            isCanBeConsumed = false
        }

        val misusedDependencies = tasks.register<DependencyMisuseAggregateReportTask>("misusedDependenciesReport") {
            dependsOn(dependencyReports)

            unusedDependencyReports = dependencyReports
            projectReport.set(project.layout.buildDirectory.file("$ROOT_DIR/misused-dependencies.txt"))
            projectReportPretty.set(project.layout.buildDirectory.file("$ROOT_DIR/misused-dependencies-pretty.txt"))
        }
        val abiReport = tasks.register<AbiAnalysisAggregateReportTask>("abiReport") {
            dependsOn(abiReportsConf)

            abiReports = abiReportsConf
            projectReport.set(project.layout.buildDirectory.file("$ROOT_DIR/abi.txt"))
            projectReportPretty.set(project.layout.buildDirectory.file("$ROOT_DIR/abi-pretty.txt"))
        }

        tasks.register("buildHealth") {
            dependsOn(misusedDependencies, abiReport)

            group = "verification"
            description = "Executes ${misusedDependencies.name} and ${abiReport.name} tasks"

            doLast {
                logger.quiet("Mis-used Dependencies report: ${misusedDependencies.get().projectReport.get().asFile.path}")
                logger.quiet("ABI report: ${abiReport.get().projectReport.get().asFile.path}")
            }
        }
    }

    private interface DependencyAnalyzer<T : ClassAnalysisTask> {
        /**
         * E.g., `flavorDebug`
         */
        val variantName: String
        /**
         * E.g., `FlavorDebug`
         */
        val variantNameCapitalized: String
        val compileConfigurationName: String
        val runtimeConfigurationName: String
        val attribute: Attribute<String>
        val attributeValue: String

        // 1.
        // This produces a report that lists all of the used classes (FQCN) in the project
        fun registerClassAnalysisTask(): TaskProvider<out T>

        // This is a no-op for com.android.application projects, since they have no meaningful ABI
        fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>): TaskProvider<AbiAnalysisTask>? = null
    }

    /**
     * Base class for analyzing an Android project (com.android.application or com.android.library only).
     */
    private abstract class AndroidAnalyzer<T : ClassAnalysisTask>(
        protected val project: Project,
        protected val variant: BaseVariant
    ) : DependencyAnalyzer<T> {

        final override val variantName: String = variant.name
        final override val variantNameCapitalized: String = variantName.capitalize()
        final override val compileConfigurationName = "${variantName}CompileClasspath"
        final override val runtimeConfigurationName = "${variantName}RuntimeClasspath"
        final override val attribute: Attribute<String> = AndroidArtifacts.ARTIFACT_TYPE
        final override val attributeValue = "android-classes"

        protected fun getKaptStubs() = getKaptStubs(project, variantName)
    }

    private class AndroidLibAnalyzer(
        project: Project, variant: BaseVariant
    ) : AndroidAnalyzer<JarAnalysisTask>(project, variant) {

        // Known to exist in AGP 3.5 and 3.6
        private fun getBundleTask() =
            project.tasks.named("bundleLibCompile$variantNameCapitalized", BundleLibraryClasses::class.java)

        override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
            // Known to exist in AGP 3.5 and 3.6
            val bundleTask =
                project.tasks.named("bundleLibCompile$variantNameCapitalized", BundleLibraryClasses::class.java)

            return project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
                jar.set(bundleTask.flatMap { it.output })
                kaptJavaStubs.from(getKaptStubs())
                layouts(variant.sourceSets.flatMap { it.resDirectories })

                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }

        override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
            project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
                jar.set(getBundleTask().flatMap { it.output })
                dependencies.set(dependencyReportTask.flatMap { it.output })

                output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
                abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
            }
    }

    private class AndroidAppAnalyzer(
        project: Project, variant: BaseVariant
    ) : AndroidAnalyzer<ClassListAnalysisTask>(project, variant) {

        override fun registerClassAnalysisTask(): TaskProvider<ClassListAnalysisTask> {
            // Known to exist in Kotlin 1.3.50.
            val kotlinCompileTask = project.tasks.named("compile${variantNameCapitalized}Kotlin") // KotlinCompile
            // Known to exist in AGP 3.5 and 3.6, albeit with different backing classes (AndroidJavaCompile and JavaCompile)
            val javaCompileTask = project.tasks.named("compile${variantNameCapitalized}JavaWithJavac")

            return project.tasks.register<ClassListAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
                kotlinClasses.from(kotlinCompileTask.get().outputs.files.asFileTree)
                javaClasses.from(javaCompileTask.get().outputs.files.asFileTree)
                kaptJavaStubs.from(getKaptStubs())
                layouts(variant.sourceSets.flatMap { it.resDirectories })

                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }
    }

    private class JavaLibAnalyzer(
        private val project: Project,
        private val sourceSet: SourceSet
    ) : DependencyAnalyzer<JarAnalysisTask> {

        override val variantName: String = sourceSet.name
        override val variantNameCapitalized = variantName.capitalize()
        // Yes, these two are the same for this case
        override val compileConfigurationName = "compileClasspath"
        override val runtimeConfigurationName = compileConfigurationName
        override val attribute: Attribute<String> = Attribute.of("artifactType", String::class.java)
        override val attributeValue = "jar"

        private fun getJarTask() = project.tasks.named(sourceSet.jarTaskName, Jar::class.java)

        override fun registerClassAnalysisTask(): TaskProvider<JarAnalysisTask> {
            return project.tasks.register<JarAnalysisTask>("analyzeClassUsage$variantNameCapitalized") {
                jar.set(getJarTask().flatMap { it.archiveFile })
                kaptJavaStubs.from(getKaptStubs(project, variantName))
                output.set(project.layout.buildDirectory.file(getAllUsedClassesPath(variantName)))
            }
        }

        override fun registerAbiAnalysisTask(dependencyReportTask: TaskProvider<DependencyReportTask>) =
            project.tasks.register<AbiAnalysisTask>("abiAnalysis$variantNameCapitalized") {
                jar.set(getJarTask().flatMap { it.archiveFile })
                dependencies.set(dependencyReportTask.flatMap { it.output })

                output.set(project.layout.buildDirectory.file(getAbiAnalysisPath(variantName)))
                abiDump.set(project.layout.buildDirectory.file(getAbiDumpPath(variantName)))
            }
    }
}

// Best guess as to path to kapt-generated Java stubs
private fun getKaptStubs(project: Project, variantName: String) = project.layout.buildDirectory.asFileTree.matching {
    include("**/kapt*/**/${variantName}/**/*.java")
}

private const val ROOT_DIR = "dependency-analysis"

private fun getVariantDirectory(variantName: String) = "$ROOT_DIR/$variantName"

private fun getArtifactsPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts.json"

private fun getArtifactsPrettyPath(variantName: String) = "${getVariantDirectory(variantName)}/artifacts-pretty.json"

private fun getAllUsedClassesPath(variantName: String) = "${getVariantDirectory(variantName)}/all-used-classes.txt"

private fun getAllDeclaredDepsPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies.json"

private fun getAllDeclaredDepsPrettyPath(variantName: String) =
    "${getVariantDirectory(variantName)}/all-declared-dependencies-pretty.json"

private fun getUnusedDirectDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/unused-direct-dependencies.json"

private fun getUsedTransitiveDependenciesPath(variantName: String) =
    "${getVariantDirectory(variantName)}/used-transitive-dependencies.json"

private fun getMisusedDependenciesHtmlPath(variantName: String) =
    "${getVariantDirectory(variantName)}/misused-dependencies.html"

private fun getAbiAnalysisPath(variantName: String) = "${getVariantDirectory(variantName)}/abi.txt"

private fun getAbiDumpPath(variantName: String) = "${getVariantDirectory(variantName)}/abi-dump.txt"
