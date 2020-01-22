@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
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
private const val CONF_ADVICE_REPORT = "adviceReport"

internal const val TASK_GROUP_DEP = "dependency-analysis"

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
                val androidClassAnalyzer = AndroidAppAnalyzer(
                    this@configureAndroidAppProject,
                    this,
                    ANDROID_GRADLE_PLUGIN_VERSION
                )
                analyzeDependencies(androidClassAnalyzer)
            }
        }
    }

    /**
     * Has the `com.android.library` plugin applied.
     */
    private fun Project.configureAndroidLibProject() {
        the<LibraryExtension>().libraryVariants.all {
            val androidClassAnalyzer = AndroidLibAnalyzer(
                this@configureAndroidLibProject,
                this,
                ANDROID_GRADLE_PLUGIN_VERSION
            )
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
        val adviceReportsConf = configurations.create(CONF_ADVICE_REPORT) {
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
        val adviceReport = tasks.register<AdviceAggregateReportTask>("adviceReport") {
            dependsOn(adviceReportsConf)

            adviceReports = adviceReportsConf
            projectReport.set(project.layout.buildDirectory.file(getAdviceAggregatePath()))
            projectReportPretty.set(project.layout.buildDirectory.file(getAdviceAggregatePrettyPath()))
        }

        // This task will always print to console, which is the goal.
        tasks.register("buildHealth") {
            dependsOn(misusedDependencies, abiReport, adviceReport)

            group = TASK_GROUP_DEP
            description = "Executes ${misusedDependencies.name}, ${abiReport.name}, and ${adviceReport.name} tasks"

            doLast {
                logger.quiet("Mis-used Dependencies report: ${misusedDependencies.get().projectReport.get().asFile.path}")
                logger.quiet("            (pretty-printed): ${misusedDependencies.get().projectReportPretty.get().asFile.path}")
                logger.quiet("ABI report                  : ${abiReport.get().projectReport.get().asFile.path}")
                logger.quiet("            (pretty-printed): ${abiReport.get().projectReportPretty.get().asFile.path}")
                logger.quiet("Advice report               : ${adviceReport.get().projectReport.get().asFile.path}")
                logger.quiet("            (pretty-printed): ${adviceReport.get().projectReportPretty.get().asFile.path}")
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

            setArtifacts(artifactCollection)

            val dependencyConfs = ConfigurationsToDependenciesTransformer(variantName, project)
                .dependencyConfigurations()
            dependencyConfigurations.set(dependencyConfs)

            output.set(layout.buildDirectory.file(getArtifactsPath(variantName)))
            outputPretty.set(layout.buildDirectory.file(getArtifactsPrettyPath(variantName)))
        }

        // Produces a report that lists all dependencies, whether or not they're transitive, and associated with the
        // classes they contain.
        val dependencyReportTask =
            tasks.register<DependencyReportTask>("dependenciesReport$variantTaskName") {
                val runtimeClasspath = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
                configuration = runtimeClasspath
                artifactFiles.setFrom(
                    runtimeClasspath.incoming.artifactView {
                        attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
                    }.artifacts.artifactFiles
                )

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

        // Produces a report that lists all dependencies that contributed _used_ Android resources (based on a
        // best-guess heuristic). Is null for java-library projects.
        val androidResUsageTask = dependencyAnalyzer.registerAndroidResAnalysisTask()

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
            androidResUsageTask?.let { task ->
                usedAndroidResDependencies.set(task.flatMap { it.usedAndroidResDependencies })
            }

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

        // Combine "misused dependencies" and abi reports into a single piece of advice for how to alter one's
        // dependencies
        val adviceTask = tasks.register<AdviceTask>("advice$variantTaskName") {
            unusedDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUnusedDependencies })
            usedTransitiveDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUsedTransitives })
            abiAnalysisTask?.let { task ->
                abiDependenciesReport.set(task.flatMap { it.output })
            }
            allDeclaredDependenciesReport.set(artifactsReportTask.flatMap { it.output })

            adviceReport.set(layout.buildDirectory.file(getAdvicePath(variantName)))
        }

        // Adds terminal artifacts to custom configurations to be consumed by root project for aggregate reports.
        maybeAddArtifact(misusedDependenciesTask, abiAnalysisTask, adviceTask, variantName)
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
        adviceTask: TaskProvider<AdviceTask>,
        variantName: String
    ) {
        // We must only do this once per project
        if (!shouldAddArtifact(variantName)) {
            return
        }
        artifactAdded.set(true)

        // Configure misused dependencies aggregate and advice tasks
        val dependencyReportsConf = configurations.create(CONF_DEPENDENCY_REPORT) {
            isCanBeResolved = false
        }
        val adviceReportsConf = configurations.create(CONF_ADVICE_REPORT) {
            isCanBeResolved = false
        }
        artifacts {
            add(dependencyReportsConf.name, layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))) {
                builtBy(misusedDependenciesTask)
            }
            add(adviceReportsConf.name, layout.buildDirectory.file(getAdvicePath(variantName))) {
                builtBy(adviceTask)
            }
        }
        // Add project dependency on root project to this project, with our new configurations
        rootProject.dependencies {
            add(dependencyReportsConf.name, project(this@maybeAddArtifact.path, dependencyReportsConf.name))
            add(adviceReportsConf.name, project(this@maybeAddArtifact.path, adviceReportsConf.name))
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
