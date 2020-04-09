@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.autonomousapps.internal.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.utils.log
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
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

private const val CONF_DEPENDENCY_REPORT_CONSUMER = "dependencyReportConsumer"
private const val CONF_ABI_REPORT_CONSUMER = "abiReportConsumer"
private const val CONF_ADVICE_REPORT_CONSUMER = "adviceReportConsumer"

private const val CONF_DEPENDENCY_REPORT_PRODUCER = "dependencyReportProducer"
private const val CONF_ABI_REPORT_PRODUCER = "abiReportProducer"
private const val CONF_ADVICE_REPORT_PRODUCER = "adviceReportProducer"

internal const val TASK_GROUP_DEP = "dependency-analysis"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

  /**
   * Used for validity check.
   */
  private fun Project.getExtensionOrNull(): DependencyAnalysisExtension? = rootProject.extensions.findByType()

  /**
   * Used after validity check, when it must be non-null.
   */
  private fun Project.getExtension(): DependencyAnalysisExtension = getExtensionOrNull()!!

  private val artifactAdded = AtomicBoolean(false)

  private lateinit var inMemoryCacheProvider: Provider<InMemoryCache>

  override fun apply(project: Project): Unit = project.run {
    checkAgpVersion()

    registerInMemoryCache()

    if (this == rootProject) {
      logger.log("Adding root project tasks")

      extensions.create<DependencyAnalysisExtension>(EXTENSION_NAME, objects)
      configureRootProject()
      conditionallyApplyToSubprojects()
    }

    checkPluginWasAppliedToRoot()

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
      configureJavaLibProject()
    }
  }

  private fun Project.checkAgpVersion() {
    val current = try {
      AgpVersion.current()
    } catch (_: Throwable) {
      logger.info("AGP not on classpath; assuming non-Android project")
      return
    }

    logger.debug("AgpVersion = $current")
    if (!current.isSupported() && this == rootProject) {
      logger.warn(
        "This plugin is only known to work with versions of AGP between ${AgpVersion.AGP_MIN.version} and ${AgpVersion.AGP_MAX.version}. You are using ${current.version}. Proceed at your own risk."
      )
    }
  }

  private fun Project.registerInMemoryCache() {
    inMemoryCacheProvider = gradle.sharedServices.registerIfAbsent("inMemoryCache", InMemoryCache::class.java) {}
  }

  private fun Project.checkPluginWasAppliedToRoot() {
    if (getExtensionOrNull() == null) {
      throw GradleException("You must apply the plugin to the root project. Current project is $path")
    }
  }

  /**
   * Only apply to all subprojects if user hasn't requested otherwise. See [DependencyAnalysisExtension.autoApply].
   */
  private fun Project.conditionallyApplyToSubprojects() {
    // Must be inside afterEvaluate to access user configuration
    afterEvaluate {
      if (getExtension().autoApply.get()) {
        logger.debug("Applying plugin to all subprojects")
        subprojects {
          logger.debug("Auto-applying to $path.")
          apply(plugin = "com.autonomousapps.dependency-analysis")
        }
      } else {
        logger.debug("Not applying plugin to all subprojects. User must apply to each manually")
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
      val appExtension = the<AppExtension>()
      appExtension.applicationVariants.all {
        val androidClassAnalyzer = AndroidAppAnalyzer(
          this@configureAndroidAppProject,
          this,
          AgpVersion.current().version
        )
        analyzeDependencies(androidClassAnalyzer)
      }
    }
  }

  /**
   * Has the `com.android.library` plugin applied.
   */
  private fun Project.configureAndroidLibProject() {
    val libExtension = the<LibraryExtension>()
    libExtension.libraryVariants.all {
      val androidClassAnalyzer = AndroidLibAnalyzer(
        this@configureAndroidLibProject,
        this,
        AgpVersion.current().version
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
   */
  private fun Project.configureRootProject() {
    val dependencyReportsConf = configurations.create(CONF_DEPENDENCY_REPORT_CONSUMER) {
      isCanBeConsumed = false
    }
    val abiReportsConf = configurations.create(CONF_ABI_REPORT_CONSUMER) {
      isCanBeConsumed = false
    }
    val adviceReportsConf = configurations.create(CONF_ADVICE_REPORT_CONSUMER) {
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

    // Configured below
    val failOrWarn = tasks.register<FailOrWarnTask>("failOrWarn")

    val adviceReport = tasks.register<AdviceAggregateReportTask>("adviceReport") {
      dependsOn(adviceReportsConf)

      adviceReports = adviceReportsConf
      projectReport.set(project.layout.buildDirectory.file(getAdviceAggregatePath()))
      projectReportPretty.set(project.layout.buildDirectory.file(getAdviceAggregatePrettyPath()))

      finalizedBy(failOrWarn)
    }

    // This task will always print to console, which is the goal.
    val buildHealth = tasks.register("buildHealth") {
      dependsOn(misusedDependencies, abiReport, adviceReport)

      group = TASK_GROUP_DEP
      description = "Executes ${misusedDependencies.name}, ${abiReport.name}, and ${adviceReport.name} tasks"

      finalizedBy(failOrWarn)

      doLast {
        logger.debug("Mis-used Dependencies report: ${misusedDependencies.get().projectReport.get().asFile.path}")
        logger.debug("            (pretty-printed): ${misusedDependencies.get().projectReportPretty.get().asFile.path}")
        logger.debug("ABI report                  : ${abiReport.get().projectReport.get().asFile.path}")
        logger.debug("            (pretty-printed): ${abiReport.get().projectReportPretty.get().asFile.path}")

        logger.quiet("Advice report (aggregated): ${adviceReport.get().projectReport.get().asFile.path}")
        logger.quiet("(pretty-printed)          : ${adviceReport.get().projectReportPretty.get().asFile.path}")
      }
    }

    failOrWarn.configure {
      shouldRunAfter(buildHealth)

      advice.set(adviceReport.flatMap { it.projectReport })

      with(getExtension().issueHandler) {
        failOnAny.set(anyIssue.behavior)
        failOnUnusedDependencies.set(unusedDependenciesIssue.behavior)
        failOnUsedTransitiveDependencies.set(usedTransitiveDependenciesIssue.behavior)
        failOnIncorrectConfiguration.set(incorrectConfigurationIssue.behavior)
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

    // Produces a report that lists all direct and transitive dependencies, their artifacts
    val artifactsReportTask = tasks.register<ArtifactsAnalysisTask>("artifactsReport$variantTaskName") {
      val artifactCollection =
        configurations[dependencyAnalyzer.compileConfigurationName].incoming.artifactView {
          attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
        }.artifacts

      setArtifacts(artifactCollection)

      val dependencyConfs = ConfigurationsToDependenciesTransformer(variantName, project).dependencyConfigurations()
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

        allComponentsReport.set(layout.buildDirectory.file(getAllDeclaredDepsPath(variantName)))
        allComponentsReportPretty.set(layout.buildDirectory.file(getAllDeclaredDepsPrettyPath(variantName)))

        inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
      }

    // Produces a report that lists all import declarations in the source of the current project. This report is
    // consumed by (at time of writing) inlineTask and constantTask.
    val importFinderTask = tasks.register<ImportFinderTask>("importFinder$variantTaskName") {
      javaSourceFiles.setFrom(dependencyAnalyzer.javaSourceFiles)
      kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
      importsReport.set(layout.buildDirectory.file(getImportsPath(variantName)))
    }

    // Produces a report that lists all dependencies that contributed inline members used by the current project.
    val inlineTask = tasks.register<InlineMemberExtractionTask>("inlineMemberExtractor$variantTaskName") {
      artifacts.set(artifactsReportTask.flatMap { it.output })
      imports.set(importFinderTask.flatMap { it.importsReport })
      inlineMembersReport.set(layout.buildDirectory.file(getInlineMembersPath(variantName)))
      inlineUsageReport.set(layout.buildDirectory.file(getInlineUsagePath(variantName)))

      inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
    }

    // Produces a report that lists all dependencies that contributed constants used by the current project.
    val constantTask = tasks.register<ConstantUsageDetectionTask>("constantUsageDetector$variantTaskName") {
      artifacts.set(artifactsReportTask.flatMap { it.output })
      imports.set(importFinderTask.flatMap { it.importsReport })
      constantUsageReport.set(layout.buildDirectory.file(getConstantUsagePath(variantName)))

      inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
    }

    // Produces a report of packages from included manifests. Is null for java-library projects.
    val manifestPackageExtractionTask = dependencyAnalyzer.registerManifestPackageExtractionTask()

    // Produces a report that lists all dependencies that contribute Android resources that are used by Java/Kotlin
    // source (based on a best-guess heuristic). Is null for java-library projects.
    val androidResBySourceUsageTask = manifestPackageExtractionTask?.let {
      dependencyAnalyzer.registerAndroidResToSourceAnalysisTask(it)
    }

    // Produces a report that lists dependencies that contribute Android resources that are used by Android resources.
    // Is null for java-library projects.
    val androidResByResUsageTask = dependencyAnalyzer.registerAndroidResToResAnalysisTask()

    // Produces a report that list all classes _used by_ the given project. Analyzes bytecode and collects all class
    // references.
    val analyzeClassesTask = dependencyAnalyzer.registerClassAnalysisTask()

    // A report of all unused dependencies and used-transitive dependencies
    val misusedDependenciesTask = tasks.register<DependencyMisuseTask>("misusedDependencies$variantTaskName") {
      val runtimeConfiguration = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
      artifactFiles =
        runtimeConfiguration.incoming.artifactView {
          attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
        }.artifacts.artifactFiles
      this@register.runtimeConfiguration = runtimeConfiguration

      declaredDependencies.set(dependencyReportTask.flatMap { it.allComponentsReport })
      usedClasses.set(analyzeClassesTask.flatMap { it.output })
      usedInlineDependencies.set(inlineTask.flatMap { it.inlineUsageReport })
      usedConstantDependencies.set(constantTask.flatMap { it.constantUsageReport })
      manifestPackageExtractionTask?.let { task ->
        manifests.set(task.flatMap { it.manifestPackagesReport })
      }
      androidResBySourceUsageTask?.let { task ->
        usedAndroidResBySourceDependencies.set(task.flatMap { it.usedAndroidResDependencies })
      }
      androidResByResUsageTask?.let { task ->
        usedAndroidResByResDependencies.set(task.flatMap { it.output })
      }

      outputUnusedDependencies.set(
        layout.buildDirectory.file(getUnusedDirectDependenciesPath(variantName))
      )
      outputUsedTransitives.set(
        layout.buildDirectory.file(getUsedTransitiveDependenciesPath(variantName))
      )
    }

    // A report of the project's binary API, or ABI.
    val abiAnalysisTask = dependencyAnalyzer.registerAbiAnalysisTask(dependencyReportTask)

    // Combine "misused dependencies" and abi reports into a single piece of advice for how to alter one's
    // dependencies
    val adviceTask = tasks.register<AdviceTask>("advice$variantTaskName") {
      allComponentsReport.set(dependencyReportTask.flatMap { it.allComponentsReport })
      unusedDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUnusedDependencies })
      usedTransitiveDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUsedTransitives })
      abiAnalysisTask?.let { task ->
        abiDependenciesReport.set(task.flatMap { it.output })
      }
      allDeclaredDependenciesReport.set(artifactsReportTask.flatMap { it.output })

      ignoreKtx.set(getExtension().issueHandler.ignoreKtx)
      dataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
      viewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)

      // Failure states
      with(getExtension().issueHandler) {
        failOnAny.set(anyIssue.behavior)
        failOnUnusedDependencies.set(unusedDependenciesIssue.behavior)
        failOnUsedTransitiveDependencies.set(usedTransitiveDependenciesIssue.behavior)
        failOnIncorrectConfiguration.set(incorrectConfigurationIssue.behavior)
      }

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
    val dependencyReportsConf = configurations.create(CONF_DEPENDENCY_REPORT_PRODUCER) {
      isCanBeResolved = false
    }
    val adviceReportsConf = configurations.create(CONF_ADVICE_REPORT_PRODUCER) {
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
      add(CONF_DEPENDENCY_REPORT_CONSUMER, project(this@maybeAddArtifact.path, dependencyReportsConf.name))
      add(CONF_ADVICE_REPORT_CONSUMER, project(this@maybeAddArtifact.path, adviceReportsConf.name))
    }

    // Configure ABI analysis aggregate task
    abiAnalysisTask?.let {
      val abiReportsConf = configurations.create(CONF_ABI_REPORT_PRODUCER) {
        isCanBeResolved = false
      }
      artifacts {
        add(abiReportsConf.name, layout.buildDirectory.file(getAbiAnalysisPath(variantName))) {
          builtBy(abiAnalysisTask)
        }
      }
      // Add project dependency on root project to this project, with our new configuration
      rootProject.dependencies {
        add(CONF_ABI_REPORT_CONSUMER, project(this@maybeAddArtifact.path, abiReportsConf.name))
      }
    }
  }

  private fun Project.shouldAddArtifact(variantName: String): Boolean {
    if (artifactAdded.get()) {
      return false
    }

    return getExtension().getFallbacks().contains(variantName)
  }
}
