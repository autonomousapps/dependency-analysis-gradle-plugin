@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.autonomousapps.internal.*
import com.autonomousapps.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.util.concurrent.atomic.AtomicBoolean

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val JAVA_LIBRARY_PLUGIN = "java-library"

private const val EXTENSION_NAME = "dependencyAnalysis"

private const val CONF_DEPENDENCY_REPORT = "dependencyReport"
private const val CONF_ABI_REPORT = "abiReport"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

    private fun Project.getExtension(): DependencyAnalysisExtension? =
        rootProject.extensions.findByType()

    private val artifactAdded = AtomicBoolean(false)

    override fun apply(project: Project): Unit = project.run {
        pluginManager.withPlugin(ANDROID_APP_PLUGIN) {
            logger.log("Adding Android tasks to ${project.path}")
            configureAndroidAppProject()
        }
        pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN) {
            logger.log("Adding Android tasks to ${project.path}")
            configureAndroidLibProject()
        }
        pluginManager.withPlugin(JAVA_LIBRARY_PLUGIN) {
            logger.log("Adding JVM tasks to ${project.path}")
            // for Java library projects, use a different convention
            getExtension()?.theVariants?.convention(listOf(JAVA_LIB_SOURCE_SET_DEFAULT))
            configureJavaLibProject()
        }

        if (this == rootProject) {
            logger.log("Adding root project tasks")

            extensions.create<DependencyAnalysisExtension>(EXTENSION_NAME, objects)
            configureRootProject()
            subprojects {
                apply(plugin = "com.autonomousapps.dependency-analysis")
            }
        }
    }

    /**
     * Has the `com.android.application` plugin applied.
     */
    private fun Project.configureAndroidAppProject() {
        // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks. This is due to use of the
        // pluginManager.withPlugin API. Currently configuring the com.android.application plugin, not any Kotlin
        // plugin. I do not know how to wait for both plugins to be ready.
        afterEvaluate {
            the<AppExtension>().applicationVariants.all {
                val androidClassAnalyzer = AndroidAppAnalyzer(this@configureAndroidAppProject, this)
                analyzeDependencies(androidClassAnalyzer)
            }
        }
    }

    /**
     * Has the `com.android.library` plugin applied.
     */
    private fun Project.configureAndroidLibProject() {
        the<LibraryExtension>().libraryVariants.all {
            val androidClassAnalyzer = AndroidLibAnalyzer(this@configureAndroidLibProject, this)
            analyzeDependencies(androidClassAnalyzer)
        }
    }

    /**
     * Has the `java-library` plugin applied.
     */
    private fun Project.configureJavaLibProject() {
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
     * Root project. Configures lifecycle tasks that aggregates reports across all subprojects.
     *
     * TODO currently no handling if root project is also a source-containing project.
     */
    private fun Project.configureRootProject() {
        val dependencyReportsConf = configurations.create(CONF_DEPENDENCY_REPORT) {
            isCanBeConsumed = false
        }
        val abiReportsConf = configurations.create(CONF_ABI_REPORT) {
            isCanBeConsumed = false
        }

        val misusedDependencies = tasks.register<DependencyMisuseAggregateReportTask>("misusedDependenciesReport") {
            dependsOn(dependencyReportsConf)

            unusedDependencyReports = dependencyReportsConf
            projectReport.set(project.layout.buildDirectory.file(getMisusedDependenciesAggregatePath()))
            projectReportPretty.set(project.layout.buildDirectory.file(getMisusedDependenciesAggregatePrettyPath()))
        }
        val abiReport = tasks.register<AbiAnalysisAggregateReportTask>("abiReport") {
            dependsOn(abiReportsConf)

            abiReports = abiReportsConf
            projectReport.set(project.layout.buildDirectory.file(getAbiAggregatePath()))
            projectReportPretty.set(project.layout.buildDirectory.file(getAbiAggregatePrettyPath()))
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

    /* ===============================================
     * The main work of the plugin happens below here.
     * ===============================================
     */

    /**
     * Tasks are registered here. This function is called in a loop, once for each Android variant or Java source set.
     */
    private fun <T : ClassAnalysisTask> Project.analyzeDependencies(dependencyAnalyzer: DependencyAnalyzer<T>) {
        val variantName = dependencyAnalyzer.variantName
        val variantTaskName = dependencyAnalyzer.variantNameCapitalized

        // Produces a report that lists all direct and transitive dependencies, their artifacts, and component type
        // (library vs project)
        val artifactsReportTask = tasks.register<ArtifactsAnalysisTask>("artifactsReport$variantTaskName") {
            val artifactCollection =
                configurations[dependencyAnalyzer.compileConfigurationName].incoming.artifactView {
                    attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                }.artifacts

            // This feels like too much work to do during configuration. We're doing it because we need information from
            // Configuration objects, which are not Serializable and cannot be directly used as inputs. We could do all
            // the work in the task execution, starting with project.configurations, but that would be problematic for
            // instant execution.
            val dependencyConfs = ConfigurationsToDependenciesTransformer(variantName, project)
                .dependencyConfigurations()
            dependencyConfigurations.set(dependencyConfs)

            artifactFiles = artifactCollection.artifactFiles
            artifacts = artifactCollection

            output.set(layout.buildDirectory.file(getArtifactsPath(variantName)))
            outputPretty.set(layout.buildDirectory.file(getArtifactsPrettyPath(variantName)))
        }

        // Produces a report that lists all dependencies, whether or not they're transitive, and associated with the
        // classes they contain.
        val dependencyReportTask =
            tasks.register<DependencyReportTask>("dependenciesReport$variantTaskName") {
                val runtimeClasspath = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
                artifactFiles.setFrom(runtimeClasspath.incoming.artifactView {
                    attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                }.artifacts.artifactFiles)
                configuration = runtimeClasspath
                allArtifacts.set(artifactsReportTask.flatMap { it.output })

                output.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantName)))
                outputPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantName)))
            }

        val inlineTask = tasks.register<InlineMemberExtractionTask>("inlineMemberExtractor$variantTaskName") {
            artifacts.set(artifactsReportTask.flatMap { it.output })
            kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
            inlineMembersReport.set(layout.buildDirectory.file(getInlineMembersPath(variantName)))
            inlineUsageReport.set(layout.buildDirectory.file(getInlineUsagePath(variantName)))
        }

        // Produces a report that list all classes _used_ by the given project. Analyzes bytecode and collects all class
        // references.
        val analyzeClassesTask = dependencyAnalyzer.registerClassAnalysisTask()

        // A terminal report. All unused dependencies and used-transitive dependencies.
        val misusedDependenciesTask = tasks.register<DependencyMisuseTask>("misusedDependencies$variantTaskName") {
            artifactFiles =
                configurations.getByName(dependencyAnalyzer.runtimeConfigurationName).incoming.artifactView {
                    attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                }.artifacts.artifactFiles
            configurationName.set(dependencyAnalyzer.runtimeConfigurationName)
            declaredDependencies.set(dependencyReportTask.flatMap { it.output })
            usedClasses.set(analyzeClassesTask.flatMap { it.output })
            usedInlineDependencies.set(inlineTask.flatMap { it.inlineUsageReport })

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

        // A terminal report. A projects binary API, or ABI.
        val abiAnalysisTask = dependencyAnalyzer.registerAbiAnalysisTask(dependencyReportTask)

        // Adds terminal artifacts to custom configurations to be consumed by root project for aggregate reports.
        maybeAddArtifact(misusedDependenciesTask, abiAnalysisTask, variantName)
    }

    /**
     * Creates `dependencyReport` and `abiReport` configurations on project, and adds those reports as artifacts to
     * those configurations, for consumption by the root project when generating aggregate reports.
     *
     * "Maybe" because we only do this once per project. This functions ensures it will only happen once. Every other
     * time, it's a no-op.
     */
    private fun Project.maybeAddArtifact(
        misusedDependenciesTask: TaskProvider<DependencyMisuseTask>,
        abiAnalysisTask: TaskProvider<AbiAnalysisTask>?,
        variantName: String
    ) {
        // We must only do this once per project
        if (!shouldAddArtifact(variantName)) {
            return
        }
        artifactAdded.set(true)

        // Configure misused dependencies aggregate tasks
        val dependencyReportsConf = configurations.create(CONF_DEPENDENCY_REPORT) {
            isCanBeResolved = false
        }
        artifacts {
            add(dependencyReportsConf.name, layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))) {
                builtBy(misusedDependenciesTask)
            }
        }
        // Add project dependency on root project to this project, with our new configuration
        rootProject.dependencies {
            add(dependencyReportsConf.name, project(this@maybeAddArtifact.path, dependencyReportsConf.name))
        }

        // Configure ABI analysis aggregate task
        abiAnalysisTask?.let {
            val abiReportsConf = configurations.create(CONF_ABI_REPORT) {
                isCanBeResolved = false
            }
            artifacts {
                add(abiReportsConf.name, layout.buildDirectory.file(getAbiAnalysisPath(variantName))) {
                    builtBy(abiAnalysisTask)
                }
            }
            // Add project dependency on root project to this project, with our new configuration
            rootProject.dependencies {
                add(abiReportsConf.name, project(this@maybeAddArtifact.path, abiReportsConf.name))
            }
        }
    }

    private fun Project.shouldAddArtifact(variantName: String): Boolean {
        if (artifactAdded.get()) {
            return false
        }

        return getExtension()?.getFallbacks()?.contains(variantName) == true
    }
}
