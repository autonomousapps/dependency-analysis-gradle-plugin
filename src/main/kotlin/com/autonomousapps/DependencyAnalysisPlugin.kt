@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.builder.model.SourceProvider
import com.autonomousapps.internal.*
import com.autonomousapps.internal.analyzer.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.log
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.*
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.concurrent.atomic.AtomicBoolean

//region constants
private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"

private const val APPLICATION_PLUGIN = "application"
private const val JAVA_LIBRARY_PLUGIN = "java-library"
private const val JAVA_PLUGIN = "java"
private const val SPRING_BOOT_PLUGIN = "org.springframework.boot"

/** This plugin can be applied along with java-library, so needs special care */
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"

private const val EXTENSION_NAME = "dependencyAnalysis"
private const val SHARED_SERVICES_IN_MEMORY_CACHE = "inMemoryCache"

private const val CONF_ADVICE_ALL_CONSUMER = "adviceAllConsumer"
private const val CONF_ADVICE_ALL_PRODUCER = "adviceAllProducer"

internal const val TASK_GROUP_DEP = "dependency-analysis"
//endregion

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

  /**
   * Used for validity check.
   */
  private fun Project.getExtensionOrNull(): DependencyAnalysisExtension? =
    rootProject.extensions.findByType()

  /**
   * Used after validity check, when it must be non-null.
   */
  private fun Project.getExtension(): DependencyAnalysisExtension = getExtensionOrNull()!!

  /**
   * Used by non-root projects.
   */
  private var subExtension: DependencyAnalysisSubExtension? = null

  /**
   * Used as a gate to prevent this plugin from configuring a project more than once. If ever
   * checked and the value is already `true`, creates and configures the [RedundantPluginSubPlugin].
   */
  private val configuredForKotlinJvmOrJavaLibrary = AtomicBoolean(false)

  /**
   * Used as a gate to prevent this plugin from configuring an app project more than once. This has
   * been added because we now react to the plain ol' `java` plugin, in order to be able to analyze
   * Spring Boot projects. However, both the `application` and `java-library` plugins also apply
   * `java`, so we have to prevent double-configuration.
   */
  private val configuredForJavaProject = AtomicBoolean(false)

  private lateinit var inMemoryCacheProvider: Provider<InMemoryCache>
  private lateinit var aggregateAdviceTask: TaskProvider<AdviceSubprojectAggregationTask>
  private lateinit var aggregateGraphTask: TaskProvider<DependencyGraphAggregationTask>
  private lateinit var aggregateReasonTask: TaskProvider<ReasonAggregationTask>

  companion object {
    private val JAVA_COMPARATOR by lazy {
      Comparator<SourceProvider> { s1, s2 -> s1.name.compareTo(s2.name) }
    }
    private val KOTLIN_COMPARATOR by lazy {
      Comparator<KotlinSourceSet> { s1, s2 -> s1.name.compareTo(s2.name) }
    }
  }

  override fun apply(project: Project): Unit = project.run {
    checkAgpVersion()
    registerInMemoryCache()

    if (this == rootProject) {
      logger.log("Adding root project tasks")

      extensions.create<DependencyAnalysisExtension>(EXTENSION_NAME, objects)
      configureRootProject()
      conditionallyApplyToSubprojects()

      gradle.taskGraph.addTaskExecutionListener(UnitTestCompilationFailureListener(project.logger))
    }

    checkPluginWasAppliedToRoot()

    if (this != rootProject) {
      val rootExtProvider = {
        extensions.findByType<DependencyAnalysisExtension>()!!
      }
      subExtension = extensions.create(EXTENSION_NAME, objects, rootExtProvider, path)//getExtension(), path)
    }

    aggregateAdviceTask = tasks.register<AdviceSubprojectAggregationTask>("aggregateAdvice")
    aggregateGraphTask = tasks.register<DependencyGraphAggregationTask>("graph")
    aggregateReasonTask = tasks.register<ReasonAggregationTask>("reason")

    pluginManager.withPlugin(ANDROID_APP_PLUGIN) {
      logger.log("Adding Android tasks to ${project.path}")
      configureAndroidAppProject()
    }
    pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN) {
      logger.log("Adding Android tasks to ${project.path}")
      configureAndroidLibProject()
    }
    pluginManager.withPlugin(APPLICATION_PLUGIN) {
      logger.log("Adding JVM tasks to ${project.path}")
      configureJavaAppProject()
    }
    pluginManager.withPlugin(JAVA_LIBRARY_PLUGIN) {
      logger.log("Adding JVM tasks to ${project.path}")
      configureJavaLibProject()
    }
    pluginManager.withPlugin(KOTLIN_JVM_PLUGIN) {
      logger.log("Adding Kotlin-JVM tasks to ${project.path}")
      configureKotlinJvmProject()
    }
    pluginManager.withPlugin(JAVA_PLUGIN) {
      afterEvaluate {
        if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
          logger.log("Adding JVM tasks to ${project.path}")
          configureJavaAppProject()
        }
      }
    }

    addAggregationTask()
//    registerFailureHandler()
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
        "The Dependency Analysis plugin is only known to work with versions of AGP between ${AgpVersion.AGP_MIN.version} and ${AgpVersion.AGP_MAX.version}. You are using ${current.version}. Proceed at your own risk."
      )
    }
  }

  private fun Project.registerInMemoryCache() {
    inMemoryCacheProvider = gradle.sharedServices
      .registerIfAbsent(SHARED_SERVICES_IN_MEMORY_CACHE, InMemoryCache::class.java) {}
  }

  private fun Project.checkPluginWasAppliedToRoot() {
    // "test" is the name of the dummy project that Kotlin DSL applies a plugin to when generating
    // script accessors
    if (getExtensionOrNull() == null && rootProject.name != "test") {
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
    // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks. This is due
    // to use of the pluginManager.withPlugin API. Currently configuring the com.android.application
    // plugin, not any Kotlin plugin. I do not know how to wait for both plugins to be ready.
    afterEvaluate {
      // If kotlin-android is applied, get the Kotlin source sets
      val kotlinSourceSets = findKotlinSourceSets()

      val appExtension = the<AppExtension>()
      appExtension.applicationVariants.all {
        // Container of all source sets relevant to this variant
        val variantSourceSet = newVariantSourceSet(sourceSets, unitTestVariant?.sourceSets, kotlinSourceSets)
        val androidClassAnalyzer = AndroidAppAnalyzer(
          project = this@configureAndroidAppProject,
          variant = this,
          agpVersion = AgpVersion.current().version,
          variantSourceSet = variantSourceSet
        )
        analyzeDependencies(androidClassAnalyzer)
      }
    }
  }

  /**
   * Has the `com.android.library` plugin applied.
   */
  private fun Project.configureAndroidLibProject() {
    afterEvaluate {
      // If kotlin-android is applied, get the Kotlin source sets
      val kotlinSourceSets = findKotlinSourceSets()

      the<LibraryExtension>().libraryVariants.all {
        // Container of all source sets relevant to this variant
        val variantSourceSet = newVariantSourceSet(sourceSets, unitTestVariant?.sourceSets, kotlinSourceSets)
        val androidClassAnalyzer = AndroidLibAnalyzer(
          project = this@configureAndroidLibProject,
          variant = this,
          agpVersion = AgpVersion.current().version,
          variantSourceSet = variantSourceSet
        )
        analyzeDependencies(androidClassAnalyzer)
      }
    }
  }

  private fun Project.findKotlinSourceSets(): NamedDomainObjectContainer<KotlinSourceSet>? {
    return if (pluginManager.hasPlugin("kotlin-android")) {
      the<KotlinProjectExtension>().sourceSets
    } else {
      null
    }
  }

  private fun newVariantSourceSet(
    androidSourceSets: List<SourceProvider>,
    androidUnitTestSourceSets: List<SourceProvider>?,
    kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>?
  ): VariantSourceSet {
    val allAndroid = androidSourceSets + (androidUnitTestSourceSets ?: emptyList())
    return VariantSourceSet(
      androidSourceSets = allAndroid.toSortedSet(JAVA_COMPARATOR),
      kotlinSourceSets = kotlinSourceSets?.filterToOrderedSet(KOTLIN_COMPARATOR) { k ->
        allAndroid.any { it.name == k.name }
      }
    )
  }

  // Scenarios
  // 1.  Has application, and then kotlin-jvm applied (in that order):
  //     - should be a kotlin-jvm-app project
  //     - must use afterEvaluate to see if kotlin-jvm is applied
  // 2.  Has kotlin-jvm, and then application applied (in that order):
  //     - should be a kotlin-jvm-app project
  //     - must use afterEvaluate to see if app or lib type project
  // 3.  Has only application applied
  //     - jvm-app project
  // 4.  Has only kotlin-jvm applied
  //     - kotlin-jvm-lib project
  // 5.  Has kotlin-jvm and java-library applied (any order)
  //     - kotlin-jvm-lib, and one is redundant (depending on source in project)
  // 6.  Has kotlin-jvm, application, and java-library applied
  //     - You're fucked, what are you even doing?
  // ***** SPRING BOOT --> Always an app project *****
  // 7.  Has Spring Boot and java applied
  //     - jvm-app project
  // 8.  Has Spring Boot and java-library applied
  //     - jvm-app project (user is wrong to use java-library)
  // 9.  Has Spring Boot, java, and java-library applied
  //     - jvm-app project
  //     - sigh
  // 10. Has Spring Boot and kotlin-jvm applied
  //     - kotlin-jvm-app project

  /**
   * Has the `application` plugin applied. The `org.jetbrains.kotlin.jvm` may or may not be applied.
   * If it is applied, this is a kotlin-jvm-app project. If it isn't, a java-jvm-app project.
   */
  private fun Project.configureJavaAppProject() {
    afterEvaluate {
      // If kotlin-jvm is NOT applied, then go ahead and configure the project as a java-jvm-app
      // project. If it IS applied, do nothing. We will configure this as a kotlin-jvm-app project
      // in `configureKotlinJvmProject()`.
      if (!pluginManager.hasPlugin(KOTLIN_JVM_PLUGIN)) {
        if (configuredForJavaProject.getAndSet(true)) {
          logger.info("(dependency analysis) $path was already configured")
          return@afterEvaluate
        }

        val java = the<JavaPluginConvention>()
        val testSource = java.sourceSets.findByName("test")
        val mainSource = java.sourceSets.findByName("main")
        mainSource?.let { sourceSet ->
          try {
            val javaModuleClassAnalyzer = JavaAppAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = testSource
            )
            analyzeDependencies(javaModuleClassAnalyzer)
          } catch (_: UnknownTaskException) {
            logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
          }
        } ?: logger.warn("No main source set. No analysis performed")
      }
    }
  }

  /**
   * Has the `java-library` plugin applied.
   */
  private fun Project.configureJavaLibProject() {
    if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured for the kotlin-jvm plugin")
      RedundantPluginSubPlugin(this, aggregateAdviceTask).configure()
      return
    }
    if (configuredForJavaProject.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured")
      return
    }

    afterEvaluate {
      val java = the<JavaPluginConvention>()
      val testSource = java.sourceSets.findByName("test")
      val mainSource = java.sourceSets.findByName("main")
      mainSource?.let { sourceSet ->
        try {
          // Regardless of the fact that this is a "java-library" project, the presence of Spring
          // Boot indicates an app project.
          val javaModuleClassAnalyzer = if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
            logger.warn(
              "(dependency analysis) You have both java-library and org.springframework.boot applied. You probably want java, not java-library."
            )
            JavaAppAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = testSource
            )
          } else {
            JavaLibAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = testSource
            )
          }
          analyzeDependencies(javaModuleClassAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
        }
      } ?: logger.warn("No main source set. No analysis performed")
    }
  }

  /**
   * Has the `org.jetbrains.kotlin.jvm` (aka `kotlin("jvm")`) plugin applied. The `application` (and
   * by implication the `java`) plugin may or may not be applied. If it is, this is an app project.
   * If it isn't, this is a library project.
   */
  private fun Project.configureKotlinJvmProject() {
    if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured for the java-library plugin")
      RedundantPluginSubPlugin(this, aggregateAdviceTask).configure()
      return
    }

    afterEvaluate {
      val kotlin = the<KotlinProjectExtension>()
      val mainSource = kotlin.sourceSets.findByName("main")
      val testSourceSet = kotlin.sourceSets.findByName("test")
      mainSource?.let { mainSourceSet ->
        try {
          val kotlinJvmModuleClassAnalyzer: KotlinJvmAnalyzer =
            if (isAppProject()) {
              KotlinJvmAppAnalyzer(this, mainSourceSet, testSourceSet)
            } else {
              KotlinJvmLibAnalyzer(this, mainSourceSet, testSourceSet)
            }
          analyzeDependencies(kotlinJvmModuleClassAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${mainSourceSet.name}`")
        }
      } ?: logger.warn("No main source set. No analysis performed")
    }
  }

  private fun Project.isAppProject() =
    pluginManager.hasPlugin(APPLICATION_PLUGIN) ||
      pluginManager.hasPlugin(SPRING_BOOT_PLUGIN) ||
      pluginManager.hasPlugin(ANDROID_APP_PLUGIN)

  /**
   * Root project. Configures lifecycle tasks that aggregates reports across all subprojects.
   */
  private fun Project.configureRootProject() {
    val outputPaths = RootOutputPaths(this)

    val adviceAllConf = configurations.create(CONF_ADVICE_ALL_CONSUMER) {
      isCanBeResolved = true
      isCanBeConsumed = false
    }

    // Configured below
    val failOrWarn = tasks.register<FailOrWarnTask>("failOrWarn")

    val adviceReport = tasks.register<AdviceAggregateReportTask>("adviceReport") {
      dependsOn(adviceAllConf)

      adviceAllReports = adviceAllConf

      projectReport.set(outputPaths.adviceAggregatePath)
      projectReportPretty.set(outputPaths.adviceAggregatePrettyPath)

      finalizedBy(failOrWarn)
    }

    // A lifecycle task
    val buildHealth = tasks.register("buildHealth") {
      dependsOn(adviceReport)

      group = TASK_GROUP_DEP
      description = "Generates holistic advice for whole project"

      finalizedBy(failOrWarn)

      doLast {
        logger.debug("Advice report (aggregated)  : ${adviceReport.get().projectReport.get().asFile.path}")
      }
    }

    // Based on user preference, will either warn of issues, or fail in the presence of them
    failOrWarn.configure {
      shouldRunAfter(buildHealth)
      advice.set(adviceReport.flatMap { it.projectReport })
      advicePretty.set(adviceReport.flatMap { it.projectReportPretty })
    }
  }

  /* ===============================================
   * The main work of the plugin happens below here.
   * ===============================================
   */

  /**
   * Tasks are registered here. This function is called in a loop, once for each Android variant or Java source set.
   */
  private fun <T : ClassAnalysisTask> Project.analyzeDependencies(
    dependencyAnalyzer: DependencyAnalyzer<T>
  ) {
    val flavorName: String? = dependencyAnalyzer.flavorName
    val variantName = dependencyAnalyzer.variantName
    val variantTaskName = dependencyAnalyzer.variantNameCapitalized
    val outputPaths = OutputPaths(this, variantName)

    // Produces a report of all declared dependencies and the configurations on which they are
    // declared
    val locateDependencies =
      tasks.register<LocateDependenciesTask>("locateDependencies$variantTaskName") {
        this@register.flavorName.set(flavorName)
        this@register.variantName.set(variantName)

        output.set(outputPaths.locationsPath)
      }

    // Produces a report that lists all direct and transitive dependencies, their artifacts
    val artifactsReportTask =
      tasks.register<ArtifactsReportTask>("artifactsReport$variantTaskName") {
        val artifactCollection =
          configurations[dependencyAnalyzer.compileConfigurationName].incoming.artifactView {
            attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
          }.artifacts

        setArtifacts(artifactCollection)
        dependencyConfigurations.set(locateDependencies.flatMap { it.output })

        output.set(outputPaths.artifactsPath)
        outputPretty.set(outputPaths.artifactsPrettyPath)
      }

    // Produces a report that lists all dependencies, whether or not they're transitive, and
    // associated with the classes they contain.
    val findClassesTask =
      tasks.register<FindClassesTask>("findClasses$variantTaskName") {
        val runtimeClasspath = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
        configuration = runtimeClasspath
        artifactFiles.setFrom(
          runtimeClasspath.incoming.artifactView {
            attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
          }.artifacts.artifactFiles
        )

        allArtifacts.set(artifactsReportTask.flatMap { it.output })

        allComponentsReport.set(outputPaths.allDeclaredDepsPath)
        allComponentsReportPretty.set(outputPaths.allDeclaredDepsPrettyPath)

        inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
      }

    // Produces a report that lists all import declarations in the source of the current project.
    // This report is consumed by (at time of writing) inlineTask and constantTask.
    val importFinderTask = tasks.register<ImportFinderTask>("importFinder$variantTaskName") {
      dependencyAnalyzer.javaSourceFiles?.let { javaSourceFiles.setFrom(it) }
      kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
      importsReport.set(outputPaths.importsPath)
    }

    // Produces a report that lists all dependencies that contributed inline members used by the
    // current project.
    val inlineTask =
      tasks.register<InlineMemberExtractionTask>("inlineMemberExtractor$variantTaskName") {
        artifacts.set(artifactsReportTask.flatMap { it.output })
        imports.set(importFinderTask.flatMap { it.importsReport })
        inlineMembersReport.set(outputPaths.inlineMembersPath)
        inlineUsageReport.set(outputPaths.inlineUsagePath)

        inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
      }

    // Produces a report that lists all dependencies that contributed constants used by the current
    // project.
    val constantTask =
      tasks.register<ConstantUsageDetectionTask>("constantUsageDetector$variantTaskName") {
        components.set(findClassesTask.flatMap { it.allComponentsReport })
        imports.set(importFinderTask.flatMap { it.importsReport })
        constantUsageReport.set(outputPaths.constantUsagePath)

        inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)
      }

    // Produces a report that list all of the dependencies that contribute types determined to be
    // used based on the presence of associated import statements. One example caught only by this
    // task: consumer project uses `Optional<Thing>` and producer project supplies Thing. Thanks to
    // type erasure, `Thing` is not present in the consumer's bytecode, so can only be detected by
    // source code analysis.
    val generalUsageTask =
      tasks.register<GeneralUsageDetectionTask>("generalsUsageDetector$variantTaskName") {
        components.set(findClassesTask.flatMap { it.allComponentsReport })
        imports.set(importFinderTask.flatMap { it.importsReport })

        output.set(outputPaths.generalUsagePath)
      }

    // Produces a report of packages from included manifests. Null for java-library projects.
    val manifestPackageExtractionTask = dependencyAnalyzer.registerManifestPackageExtractionTask()

    // Produces a report that lists all dependencies that contribute Android resources that are used
    // by Java/Kotlin source (based on a best-guess heuristic). Null for java-library projects.
    val androidResBySourceUsageTask = manifestPackageExtractionTask?.let {
      dependencyAnalyzer.registerAndroidResToSourceAnalysisTask(it)
    }

    // Produces a report that lists dependencies that contribute Android resources that are used by
    // Android resources. Is null for java-library projects.
    val androidResByResUsageTask = dependencyAnalyzer.registerAndroidResToResAnalysisTask()

    val createVariantFilesTask = dependencyAnalyzer.registerCreateVariantFilesTask()

    // Produces a report that list all classes _used by_ the given project. Analyzes bytecode and
    // collects all class references.
    val analyzeClassesTask = dependencyAnalyzer.registerClassAnalysisTask(createVariantFilesTask)

    // TODO JVM-equivalent?
    // Produces a report of all AAR dependencies with bundled native libs
    val findNativeLibsTask = dependencyAnalyzer.registerFindNativeLibsTask(locateDependencies)

    // A report of all unused dependencies and used-transitive dependencies
    val misusedDependenciesTask =
      tasks.register<DependencyMisuseTask>("misusedDependencies$variantTaskName") {
        val runtimeConfiguration = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
        artifactFiles =
          runtimeConfiguration.incoming.artifactView {
            attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
          }.artifacts.artifactFiles
        this@register.runtimeConfiguration = runtimeConfiguration

        declaredDependencies.set(findClassesTask.flatMap { it.allComponentsReport })
        usedClasses.set(analyzeClassesTask.flatMap { it.output })
        usedInlineDependencies.set(inlineTask.flatMap { it.inlineUsageReport })
        usedConstantDependencies.set(constantTask.flatMap { it.constantUsageReport })
        usedGenerally.set(generalUsageTask.flatMap { it.output })
        manifestPackageExtractionTask?.let { task ->
          manifests.set(task.flatMap { it.output })
        }
        androidResBySourceUsageTask?.let { task ->
          usedAndroidResBySourceDependencies.set(task.flatMap { it.output })
        }
        androidResByResUsageTask?.let { task ->
          usedAndroidResByResDependencies.set(task.flatMap { it.output })
        }
        findNativeLibsTask?.let { task ->
          nativeLibDependencies.set(task.flatMap { it.output })
        }

        outputAllComponents.set(outputPaths.allComponentsPath)
        outputUnusedComponents.set(outputPaths.unusedComponentsPath)
        outputUsedTransitives.set(outputPaths.usedTransitiveDependenciesPath)
        outputUsedVariantDependencies.set(outputPaths.usedVariantDependenciesPath)
      }

    val lazyAbiJson = lazy {
      with(getExtension().abiHandler.exclusionsHandler) {
        AbiExclusions(
          annotationExclusions = annotationExclusions.get(),
          classExclusions = classExclusions.get(),
          pathExclusions = pathExclusions.get()
        ).toJson()
      }
    }
    val abiExclusions = provider(lazyAbiJson::value)

    // A report of the project's binary API, or ABI.
    val abiAnalysisTask = dependencyAnalyzer.registerAbiAnalysisTask(
      findClassesTask,
      abiExclusions
    )

    // A report of service loaders
    val serviceLoaderTask =
      tasks.register<FindServiceLoadersTask>("serviceLoader$variantTaskName") {
        artifacts = configurations[dependencyAnalyzer.compileConfigurationName]
          .incoming
          .artifactView {
            attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
          }.artifacts
        dependencyConfigurations.set(locateDependencies.flatMap { it.output })
        output.set(outputPaths.serviceLoaderDependenciesPath)
      }

    // A report of unused annotation processors
    val declaredProcsTask = dependencyAnalyzer.registerFindDeclaredProcsTask(
      inMemoryCacheProvider, locateDependencies
    )
    val unusedProcsTask = dependencyAnalyzer.registerFindUnusedProcsTask(
      declaredProcsTask, importFinderTask
    )

    // A report of whether kotlin-kapt is redundant
    val kaptAlertTask = tasks.register<RedundantKaptAlertTask>(
      "redundantKaptCheck$variantTaskName"
    ) {
      kapt.set(providers.provider { project.plugins.hasPlugin("kotlin-kapt") })
      declaredProcs.set(declaredProcsTask.flatMap { it.output })
      unusedProcs.set(unusedProcsTask.flatMap { it.output })

      output.set(outputPaths.pluginKaptAdvicePath)
    }
    aggregateAdviceTask.configure {
      redundantKaptAdvice.add(kaptAlertTask.flatMap { it.output })
    }

    // Optionally transforms and prints advice to console
    val advicePrinterTask = tasks.register<AdvicePrinterTask>("advicePrinter$variantTaskName")

    // Combine "misused dependencies" and abi reports into a single piece of advice for how to alter
    // one's dependencies
    val adviceTask = tasks.register<AdviceTask>("advice$variantTaskName") {
      allComponentsReport.set(findClassesTask.flatMap { it.allComponentsReport })
      allComponentsWithTransitives.set(misusedDependenciesTask.flatMap { it.outputAllComponents })
      unusedDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUnusedComponents })
      usedTransitiveDependenciesReport.set(misusedDependenciesTask.flatMap { it.outputUsedTransitives })
      abiAnalysisTask?.let { task ->
        abiDependenciesReport.set(task.flatMap { it.output })
      }
      allDeclaredDependenciesReport.set(artifactsReportTask.flatMap { it.output })
      unusedProcsReport.set(unusedProcsTask.flatMap { it.output })
      serviceLoaders.set(serviceLoaderTask.flatMap { it.output })
      usedVariantDependencies.set(misusedDependenciesTask.flatMap { it.outputUsedVariantDependencies })

      dependenciesHandler = getExtension().dependenciesHandler

      dataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
      viewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)

      inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)

      // Failure states
      with(getExtension().issueHandler) {
        val path = this@analyzeDependencies.path

        ignoreKtx.set(ignoreKtxFor(path))
        anyBehavior.set(anyIssueFor(path))
        unusedDependenciesBehavior.set(unusedDependenciesIssueFor(path))
        usedTransitiveDependenciesBehavior.set(usedTransitiveDependenciesIssueFor(path))
        incorrectConfigurationBehavior.set(incorrectConfigurationIssueFor(path))
        compileOnlyBehavior.set(compileOnlyIssueFor(path))
        unusedProcsBehavior.set(unusedAnnotationProcessorsIssueFor(path))
      }

      adviceReport.set(outputPaths.advicePath)
      advicePrettyReport.set(outputPaths.advicePrettyPath)
      adviceConsoleReport.set(outputPaths.adviceConsolePath)
      adviceConsolePrettyReport.set(outputPaths.adviceConsolePrettyPath)
      finalizedBy(advicePrinterTask)
    }
    aggregateAdviceTask.configure {
      dependencyAdvice.add(adviceTask.flatMap { it.adviceReport })
    }

    advicePrinterTask.configure {
      adviceConsoleReport.set(adviceTask.flatMap { it.adviceConsoleReport })
      dependencyRenamingMap.set(getExtension().dependencyRenamingMap)

      adviceConsoleReportTxt.set(outputPaths.adviceConsoleTxtPath)
    }

    // TODO this should be at the aggregate/whole-project level.
    // Produces a json and graphviz file representing the dependency graph
    val graphTask = tasks.register<DependencyGraphTask>("graph$variantTaskName") {
      val runtimeClasspath = configurations.getByName(dependencyAnalyzer.runtimeConfigurationName)
      configuration = runtimeClasspath
      artifactFiles.setFrom(
        runtimeClasspath.incoming.artifactView {
          attributes.attribute(dependencyAnalyzer.attribute, dependencyAnalyzer.attributeValue)
        }.artifacts.artifactFiles
      )

      outputJson.set(outputPaths.graphPath)
      outputDot.set(outputPaths.graphDotPath)
    }
    aggregateGraphTask.configure {
      graphs.add(graphTask.flatMap { it.outputJson })
    }

    // Produces a report consolidating all information about dependencies in one place, for
    // ReasonTask to use as an input
    val reasonableDepsTask = tasks.register<ReasonableDependencyTask>("reasonableDepsReport$variantTaskName") {
      usedTransitiveComponents.set(misusedDependenciesTask.flatMap { it.outputUsedTransitives })
      components.set(findClassesTask.flatMap { it.allComponentsReport })
      abiAnalysisTask?.let { abi ->
        publicComponents.set(abi.flatMap { it.output })
      }
      inlineUsage.set(inlineTask.flatMap { it.inlineUsageReport })
      constantUsage.set(constantTask.flatMap { it.constantUsageReport })
      generalUsage.set(generalUsageTask.flatMap { it.output })
      manifestPackageExtractionTask?.let { task ->
        manifests.set(task.flatMap { it.output })
      }
      androidResByResUsageTask?.let { task ->
        resByRes.set(task.flatMap { it.output })
      }
      androidResBySourceUsageTask?.let { task ->
        resBySource.set(task.flatMap { it.output })
      }
      findNativeLibsTask?.let { task ->
        nativeDeps.set(task.flatMap { it.output })
      }
      // TODO? analyzeClassesTask -- class usages detected in proj bytecode

      output.set(outputPaths.reasonableDependenciesPath)
    }
    aggregateReasonTask.configure {
      reasonableDependenciesReports.add(reasonableDepsTask.flatMap { it.output })
    }

    // Emits to console the reason for a piece of advice
    tasks.register<ReasonTask>("reason$variantTaskName") {
      graph.set(graphTask.flatMap { it.outputJson })
      advice.set(adviceTask.flatMap { it.adviceReport })
      reasonableDependenciesReport.set(reasonableDepsTask.flatMap { it.output })

      outputDot.set(outputPaths.graphReasonPath)
    }
  }

  /**
   * This adds an aggregator task at the project level to collect all the variant-specific advice.
   */
  private fun Project.addAggregationTask() {
    val paths = NoVariantOutputPaths(this)

    // Produces a report that coalesces all the variant-specific dependency advice, as well as the
    // plugin advice, into a single report. Produces NO report if project has no source.
    aggregateAdviceTask.configure {
      onlyIf {
        dependencyAdvice.get().isNotEmpty()
          || redundantKaptAdvice.get().isNotEmpty()
          || redundantJvmAdvice.get().isNotEmpty()
      }

      output.set(paths.aggregateAdvicePath)
      outputPretty.set(paths.aggregateAdvicePrettyPath)

      inMemoryCacheProvider.set(this@DependencyAnalysisPlugin.inMemoryCacheProvider)

      with(getExtension().issueHandler) {
        val path = this@addAggregationTask.path
        anyBehavior.set(anyIssueFor(path))
        unusedDependenciesBehavior.set(unusedDependenciesIssueFor(path))
        usedTransitiveDependenciesBehavior.set(usedTransitiveDependenciesIssueFor(path))
        incorrectConfigurationBehavior.set(incorrectConfigurationIssueFor(path))
        compileOnlyBehavior.set(compileOnlyIssueFor(path))
        unusedProcsBehavior.set(unusedAnnotationProcessorsIssueFor(path))
        redundantPluginsBehavior.set(redundantPluginsIssueFor(path))
      }
    }

    // Coalesces all the variant-specific graphs into a single, perhaps illegible, whole
    aggregateGraphTask.configure {
      onlyIf {
        graphs.get().isNotEmpty()
      }
      outputJson.set(paths.aggregateGraphJsonPath)
      outputDot.set(paths.aggregateGraphDotPath)
    }

    // Permits users to reason about the entire project rather than worry about variants
    aggregateReasonTask.configure {
      onlyIf {
        reasonableDependenciesReports.get().isNotEmpty()
      }
      graph.set(aggregateGraphTask.flatMap { it.outputJson })
      comprehensiveAdvice.set(aggregateAdviceTask.flatMap { it.output })
      outputDot.set(paths.graphReasonPath)
    }

    // Is there a post-processing task? If so, run it
    afterEvaluate {
      val postProcessingTask = if (this == rootProject) {
        getExtension().postProcessingTask
      } else {
        subExtension!!.postProcessingTask
      }
      postProcessingTask?.let { task ->
        aggregateAdviceTask.configure {
          finalizedBy(task)
        }
      }
    }

    // Store the main output in the extension for consumption by end-users
    storeAdviceOutput(aggregateAdviceTask)

    // outgoing configurations, containers for our project reports for the root project to consume
    val aggregateAdviceConf = configurations.create(CONF_ADVICE_ALL_PRODUCER) {
      isCanBeResolved = false
      isCanBeConsumed = true

      outgoing.artifact(aggregateAdviceTask.flatMap { it.output })
    }

    // Remove the above artifact from the `archives` configuration (to which it is automagically
    // added), and which led to the task that produced it being made a dependency of `assemble`,
    // which led to undesirable behavior. See also https://github.com/gradle/gradle/issues/10797.
    pluginManager.withPlugin("base") {
      configurations["archives"].artifacts.clear()
    }

    // Add project dependency on root project to this project, with our new configurations
    rootProject.dependencies {
      add(CONF_ADVICE_ALL_CONSUMER, project(this@addAggregationTask.path, aggregateAdviceConf.name))
    }
  }

  // TODO this is a proof of concept
  private fun Project.registerFailureHandler() {
    gradle.buildFinished {
      val errors = inMemoryCacheProvider.get().errors()
      if (errors.isNotEmpty() && !inMemoryCacheProvider.get().hasThrown.getAndSet(true)) {
        throw GradleException("There were errors", errors.first())
      }
    }
  }

  /**
   * Stores advice output in either root extension or subproject extension.
   */
  private fun Project.storeAdviceOutput(adviceTask: TaskProvider<AdviceSubprojectAggregationTask>) {
    if (this == rootProject) {
      getExtension().storeAdviceOutput(adviceTask.flatMap { it.output })
    } else {
      subExtension!!.storeAdviceOutput(adviceTask.flatMap { it.output })
    }
  }
}
