// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.api.TestedVariant
import com.android.builder.model.SourceProvider
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.DependencyAnalysisSubExtension
import com.autonomousapps.Flags.androidIgnoredVariants
import com.autonomousapps.Flags.projectPathRegex
import com.autonomousapps.Flags.shouldAnalyzeTests
import com.autonomousapps.getExtension
import com.autonomousapps.internal.*
import com.autonomousapps.internal.GradleVersions.isAtLeastGradle82
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.analyzer.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.internal.utils.*
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.util.concurrent.atomic.AtomicBoolean

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val APPLICATION_PLUGIN = "application"
private const val JAVA_LIBRARY_PLUGIN = "java-library"
private const val JAVA_PLUGIN = "java"

private const val GRETTY_PLUGIN = "org.gretty"
private const val SPRING_BOOT_PLUGIN = "org.springframework.boot"

/** This plugin can be applied along with java-library, so needs special care */
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"

/** This "plugin" is applied to every project in a build. */
internal class ProjectPlugin(private val project: Project) {

  companion object {
    private val JAVA_COMPARATOR by unsafeLazy {
      Comparator<SourceProvider> { s1, s2 -> s1.name.compareTo(s2.name) }
    }
  }

  /** Used by non-root projects. */
  private var subExtension: DependencyAnalysisSubExtension? = null

  /**
   * Used as a gate to prevent this plugin from configuring a project more than once. If ever
   * checked and the value is already `true`, creates and configures the [RedundantJvmPlugin].
   */
  private val configuredForKotlinJvmOrJavaLibrary = AtomicBoolean(false)

  /**
   * Used as a gate to prevent this plugin from configuring an app project more than once. This has
   * been added because we now react to the plain ol' `java` plugin, in order to be able to analyze
   * Spring Boot projects. However, both the `application` and `java-library` plugins also apply
   * `java`, so we have to prevent double-configuration.
   */
  private val configuredForJavaProject = AtomicBoolean(false)

  /** We only want to register the aggregation tasks if the by-variants tasks are registered. */
  private val aggregatorsRegistered = AtomicBoolean(false)

  private lateinit var findDeclarationsTask: TaskProvider<FindDeclarationsTask>
  private lateinit var redundantJvmPlugin: RedundantJvmPlugin
  private lateinit var computeAdviceTask: TaskProvider<ComputeAdviceTask>
  private lateinit var reasonTask: TaskProvider<ReasonTask>
  private lateinit var computeResolvedDependenciesTask: TaskProvider<ComputeResolvedDependenciesTask>
  private val isDataBindingEnabled = project.objects.property<Boolean>().convention(false)
  private val isViewBindingEnabled = project.objects.property<Boolean>().convention(false)

  private val projectHealthPublisher = interProjectPublisher(
    project = project,
    artifact = DagpArtifacts.Kind.PROJECT_HEALTH
  )
  private val resolvedDependenciesPublisher = interProjectPublisher(
    project = project,
    artifact = DagpArtifacts.Kind.RESOLVED_DEPS
  )

  fun apply() = project.run {
    createSubExtension()

    // Conditionally disable analysis on some projects
    val projectPathRegex = projectPathRegex()
    if (!projectPathRegex.matches(path)) {
      logger.info("Skipping dependency analysis of project '$path'. Does not match regex '$projectPathRegex'.")
      return
    }

    // Giving up. Wrap the whole thing in afterEvaluate for simplicity and for access to user configuration via
    // extension.
    afterEvaluate {
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
        configureJavaAppProject(maybeAppProject = true)
      }
    }
  }

  private fun Project.createSubExtension() {
    if (this != rootProject) {
      // TODO this doesn't really work. Was trying to make plugin compatible with convention plugin approach
      val rootExtProvider = {
        rootProject.extensions.findByType<DependencyAnalysisExtension>()!!
      }
      subExtension = extensions.create(DependencyAnalysisExtension.NAME, objects, rootExtProvider, path)
    }
  }

  /** Has the `com.android.application` plugin applied. */
  private fun Project.configureAndroidAppProject() {
    val project = this
    val appExtension = the<AppExtension>()
    val ignoredVariantNames = androidIgnoredVariants()
    val allowedVariants = appExtension.applicationVariants.matching { variant ->
      !ignoredVariantNames.contains(variant.name)
    }

    allowedVariants.all {
      val mainSourceSets = sourceSets
      val unitTestSourceSets = if (shouldAnalyzeTests()) unitTestVariant?.sourceSets else null
      val androidTestSourceSets = if (shouldAnalyzeTests()) testVariant?.sourceSets else null

      val agpVersion = AgpVersion.current().version

      mainSourceSets.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.MAIN, sourceSets)
        val dependencyAnalyzer = AndroidAppAnalyzer(
          project = project,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }

      unitTestSourceSets?.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.TEST, sourceSets)
        val dependencyAnalyzer = AndroidAppAnalyzer(
          project = project,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }

      androidTestSourceSets?.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.ANDROID_TEST, sourceSets)
        val dependencyAnalyzer = AndroidAppAnalyzer(
          project = this@configureAndroidAppProject,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }
    }
  }

  /** Has the `com.android.library` plugin applied. */
  private fun Project.configureAndroidLibProject() {
    val project = this
    val libraryExtension = the<LibraryExtension>()
    val ignoredVariantNames = androidIgnoredVariants()
    val allowedVariants = libraryExtension.libraryVariants.matching { variant ->
      !ignoredVariantNames.contains(variant.name)
    }
    allowedVariants.all {
      val mainSourceSets = sourceSets
      val unitTestSourceSets = if (shouldAnalyzeTests()) unitTestVariant?.sourceSets else null
      val androidTestSourceSets = if (shouldAnalyzeTests()) testVariant?.sourceSets else null

      val agpVersion = AgpVersion.current().version

      mainSourceSets.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.MAIN, sourceSets)
        val dependencyAnalyzer = AndroidLibAnalyzer(
          project = project,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }

      unitTestSourceSets?.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.TEST, sourceSets)
        val dependencyAnalyzer = AndroidLibAnalyzer(
          project = project,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }

      androidTestSourceSets?.let { sourceSets ->
        val variantSourceSet = newVariantSourceSet(name, SourceSetKind.ANDROID_TEST, sourceSets)
        val dependencyAnalyzer = AndroidLibAnalyzer(
          project = project,
          variant = this,
          agpVersion = agpVersion,
          variantSourceSet = variantSourceSet
        )
        isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
        isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
        analyzeDependencies(dependencyAnalyzer)
      }
    }
  }

  private fun newVariantSourceSet(
    variantName: String,
    kind: SourceSetKind,
    androidSourceSets: List<SourceProvider>,
  ) = VariantSourceSet(
    variant = Variant(variantName, kind),
    androidSourceSets = androidSourceSets.toSortedSet(JAVA_COMPARATOR),
    compileClasspathConfigurationName = kind.compileClasspathConfigurationName(variantName),
    runtimeClasspathConfigurationName = kind.runtimeClasspathConfigurationName(variantName),
  )

  // Scenarios (this comment is a bit outdated)
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
   * Has an application-like plugin applied, such as [APPLICATION_PLUGIN], [SPRING_BOOT_PLUGIN], or [GRETTY_PLUGIN].
   *
   * The `org.jetbrains.kotlin.jvm` may or may not be applied. If it is applied, this is a kotlin-jvm-app project. If it
   * isn't, a java-jvm-app project.
   */
  private fun Project.configureJavaAppProject(maybeAppProject: Boolean = false) {
    if (maybeAppProject) {
      if (!isAppProject()) {
        // This means we only discovered the java plugin, which isn't sufficient
        return
      }
      logger.log("Adding JVM tasks to ${project.path}")
    }

    // If kotlin-jvm is NOT applied, then go ahead and configure the project as a java-jvm-app
    // project. If it IS applied, do nothing. We will configure this as a kotlin-jvm-app project
    // in `configureKotlinJvmProject()`.
    if (!pluginManager.hasPlugin(KOTLIN_JVM_PLUGIN)) {
      if (configuredForJavaProject.getAndSet(true)) {
        logger.info("(dependency analysis) $path was already configured")
        return
      }

      val j = JavaSources(this)
      j.sourceSets.forEach { sourceSet ->
        try {
          analyzeDependencies(
            JavaWithoutAbiAnalyzer(
              project = this,
              sourceSet = sourceSet,
              kind = sourceSet.jvmSourceSetKind()
            )
          )
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
        }
      }
    }
  }

  /** Has the `java-library` plugin applied. */
  private fun Project.configureJavaLibProject() {
    val j = JavaSources(this)

    configureRedundantJvmPlugin {
      it.withJava(j.hasJava)
    }

    if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured for the kotlin-jvm plugin")
      redundantJvmPlugin.configure()
      return
    }

    if (configuredForJavaProject.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured")
      return
    }

    j.sourceSets.forEach { sourceSet ->
      try {
        val kind = sourceSet.jvmSourceSetKind()
        val hasAbi = hasAbi(sourceSet)

        // Regardless of the fact that this is a "java-library" project, the presence of Spring
        // Boot indicates an app project.
        val dependencyAnalyzer = if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
          logger.warn(
            "(dependency analysis) You have both java-library and org.springframework.boot applied. You probably " +
              "want java, not java-library."
          )
          JavaWithoutAbiAnalyzer(
            project = this,
            sourceSet = sourceSet,
            kind = kind
          )
        } else {
          if (hasAbi) {
            JavaWithAbiAnalyzer(
              project = this,
              sourceSet = sourceSet,
              kind = kind,
              hasAbi = true
            )
          } else {
            JavaWithoutAbiAnalyzer(
              project = this,
              sourceSet = sourceSet,
              kind = kind
            )
          }
        }
        analyzeDependencies(dependencyAnalyzer)
      } catch (_: UnknownTaskException) {
        logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
      }
    }
  }

  /**
   * Has the `org.jetbrains.kotlin.jvm` (aka `kotlin("jvm")`) plugin applied. The `application` (and
   * by implication the `java`) plugin may or may not be applied. If it is, this is an app project.
   * If it isn't, this is a library project.
   */
  private fun Project.configureKotlinJvmProject() {
    val k = KotlinSources(this)

    configureRedundantJvmPlugin {
      it.withKotlin(k.hasKotlin)
    }

    if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
      logger.info("(dependency analysis) $path was already configured for the java-library plugin")
      redundantJvmPlugin.configure()
      return
    }

    k.sourceSets.forEach { sourceSet ->
      try {
        val kind = sourceSet.jvmSourceSetKind()
        val hasAbi = hasAbi(sourceSet)

        val dependencyAnalyzer = if (hasAbi) {
          KotlinJvmLibAnalyzer(
            project = this,
            sourceSet = sourceSet,
            kind = kind,
            hasAbi = true
          )
        } else {
          KotlinJvmAppAnalyzer(
            project = this,
            sourceSet = sourceSet,
            kind = kind
          )
        }

        analyzeDependencies(dependencyAnalyzer)
      } catch (_: UnknownTaskException) {
        logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
      }
    }
  }

  private fun Project.hasAbi(sourceSet: SourceSet): Boolean {
    if (sourceSet.name in getExtension().abiHandler.exclusionsHandler.excludedSourceSets.get()) {
      // if this sourceSet is user-excluded, then it doesn't have an ABI
      return false
    }

    val kind = sourceSet.jvmSourceSetKind()
    val hasApiConfiguration = configurations.findByName(sourceSet.apiConfigurationName) != null
    // The 'test' sourceSet does not have an ABI
    val isNotTest = kind != SourceSetKind.TEST
    // The 'main' sourceSet for an app project does not have an ABI
    val isNotMainApp = !(isAppProject() && kind == SourceSetKind.MAIN)
    return hasApiConfiguration && isNotTest && isNotMainApp
  }

  private fun Project.isAppProject() =
    pluginManager.hasPlugin(APPLICATION_PLUGIN) ||
      pluginManager.hasPlugin(SPRING_BOOT_PLUGIN) ||
      pluginManager.hasPlugin(GRETTY_PLUGIN) ||
      pluginManager.hasPlugin(ANDROID_APP_PLUGIN)

  /* ===============================================
   * The main work of the plugin happens below here.
   * ===============================================
   */

  private fun Project.configureRedundantJvmPlugin(block: (RedundantJvmPlugin) -> Unit) {
    configureAggregationTasks()

    if (!::redundantJvmPlugin.isInitialized) {
      val projectPath = this@configureRedundantJvmPlugin.path
      redundantJvmPlugin = RedundantJvmPlugin(
        project = this,
        computeAdviceTask = computeAdviceTask,
        redundantPluginsBehavior = getExtension().issueHandler.redundantPluginsIssueFor(projectPath)
      )
    }

    block(redundantJvmPlugin)
  }

  /**
   * Subproject tasks are registered here. This function is called in a loop, once for each Android variant & source
   * set, or Java source set.
   */
  private fun Project.analyzeDependencies(dependencyAnalyzer: DependencyAnalyzer) {
    configureAggregationTasks()

    val thisProjectPath = path
    val variantName = dependencyAnalyzer.variantName
    val taskNameSuffix = dependencyAnalyzer.taskNameSuffix
    val outputPaths = dependencyAnalyzer.outputPaths

    /*
     * Metadata about the dependency graph.
     */

    // Lists the dependencies declared for building the project, along with their physical artifacts (jars).
    val artifactsReport = tasks.register<ArtifactsReportTask>("artifactsReport$taskNameSuffix") {
      setClasspath(
        configurations[dependencyAnalyzer.compileConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      buildPath.set(buildPath(dependencyAnalyzer.compileConfigurationName))

      output.set(outputPaths.compileArtifactsPath)
    }

    // Lists the dependencies declared for running the project, along with their physical artifacts (jars).
    val artifactsReportRuntime = tasks.register<ArtifactsReportTask>("artifactsReportRuntime$taskNameSuffix") {
      setClasspath(
        configurations[dependencyAnalyzer.runtimeConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      buildPath.set(buildPath(dependencyAnalyzer.runtimeConfigurationName))

      output.set(outputPaths.runtimeArtifactsPath)
    }

    // Produce a DAG of the compile and runtime classpaths rooted on this project.
    val graphViewTask = tasks.register<GraphViewTask>("graphView$taskNameSuffix") {
      configureTask(
        project = this@analyzeDependencies,
        compileClasspath = configurations[dependencyAnalyzer.compileConfigurationName],
        runtimeClasspath = configurations[dependencyAnalyzer.runtimeConfigurationName],
        jarAttr = dependencyAnalyzer.attributeValueJar
      )
      buildPath.set(buildPath(dependencyAnalyzer.compileConfigurationName))
      projectPath.set(thisProjectPath)
      variant.set(variantName)
      kind.set(dependencyAnalyzer.kind)
      declarations.set(findDeclarationsTask.flatMap { it.output })

      output.set(outputPaths.compileGraphPath)
      outputDot.set(outputPaths.compileGraphDotPath)
      outputNodes.set(outputPaths.compileNodesPath)
      outputRuntime.set(outputPaths.runtimeGraphPath)
      outputRuntimeDot.set(outputPaths.runtimeGraphDotPath)
    }

    // This is an optional task that only works for Gradle 7.5+
    if (GradleVersions.isAtLeastGradle75) {
      val resolveExternalDependencies =
        tasks.register<ResolveExternalDependenciesTask>("resolveExternalDependencies$taskNameSuffix") {
          configureTask(
            project = this@analyzeDependencies,
            compileClasspath = configurations[dependencyAnalyzer.compileConfigurationName],
            runtimeClasspath = configurations[dependencyAnalyzer.runtimeConfigurationName],
            jarAttr = dependencyAnalyzer.attributeValueJar
          )
          output.set(outputPaths.externalDependenciesPath)
        }

      computeResolvedDependenciesTask.configure {
        externalDependencies.add(resolveExternalDependencies.flatMap { it.output })
      }
    }

    val computeDominatorCompile =
      tasks.register<ComputeDominatorTreeTask>("computeDominatorTreeCompile$taskNameSuffix") {
        projectPath.set(thisProjectPath)
        physicalArtifacts.set(artifactsReport.flatMap { it.output })
        graphView.set(graphViewTask.flatMap { it.output })

        outputTxt.set(outputPaths.compileDominatorConsolePath)
        outputDot.set(outputPaths.compileDominatorGraphPath)
      }

    val computeDominatorRuntime =
      tasks.register<ComputeDominatorTreeTask>("computeDominatorTreeRuntime$taskNameSuffix") {
        projectPath.set(thisProjectPath)
        physicalArtifacts.set(artifactsReportRuntime.flatMap { it.output })
        graphView.set(graphViewTask.flatMap { it.outputRuntime })

        outputTxt.set(outputPaths.runtimeDominatorConsolePath)
        outputDot.set(outputPaths.runtimeDominatorGraphPath)
      }

    // a lifecycle task that computes the dominator tree for both compile and runtime classpaths
    tasks.register("computerDominatorTree$taskNameSuffix") {
      dependsOn(computeDominatorCompile, computeDominatorRuntime)
    }

    tasks.register<PrintDominatorTreeTask>("printDominatorTreeCompile$taskNameSuffix") {
      consoleText.set(computeDominatorCompile.flatMap { it.outputTxt })
    }

    tasks.register<PrintDominatorTreeTask>("printDominatorTreeRuntime$taskNameSuffix") {
      consoleText.set(computeDominatorRuntime.flatMap { it.outputTxt })
    }

    reasonTask.configure {
      dependencyGraphViews.add(graphViewTask.flatMap { it.output /* compile graph */ })
      dependencyGraphViews.add(graphViewTask.flatMap { it.outputRuntime })
    }

    /* ******************************
     * Producers. Find the capabilities of all the producers (dependencies). There are many capabilities, including:
     * 1. Android linters.
     * 2. Classes and constants.
     * 3. Inline members from Kotlin libraries.
     * 4. Android components (e.g. Services and Providers).
     * etc.
     *
     * And then synthesize the above.
     ********************************/

    // A report of all dependencies that supply Android linters on the compile classpath.
    val androidLintTask = dependencyAnalyzer.registerFindAndroidLintersTask()

    // A report of all dependencies that supply Android assets on the compile classpath.
    val findAndroidAssetsTask = dependencyAnalyzer.registerFindAndroidAssetProvidersTask()

    // Explode jars to expose their secrets.
    val explodeJarTask = tasks.register<ExplodeJarTask>("explodeJar$taskNameSuffix") {
      inMemoryCache.set(InMemoryCache.register(project))
      compileClasspath.setFrom(
        configurations[dependencyAnalyzer.compileConfigurationName]
          .artifactsFor(dependencyAnalyzer.attributeValueJar)
          .artifactFiles
      )
      physicalArtifacts.set(artifactsReport.flatMap { it.output })
      androidLintTask?.let { task ->
        androidLinters.set(task.flatMap { it.output })
      }

      output.set(outputPaths.allDeclaredDepsPath)
    }

    // Find the inline members of this project's dependencies.
    val kotlinMagicTask = tasks.register<FindInlineMembersTask>("findInlineMembers$taskNameSuffix") {
      inMemoryCacheProvider.set(InMemoryCache.register(project))
      compileClasspath.setFrom(
        configurations[dependencyAnalyzer.compileConfigurationName]
          .artifactsFor(dependencyAnalyzer.attributeValueJar)
          .artifactFiles
      )
      artifacts.set(artifactsReport.flatMap { it.output })
      outputInlineMembers.set(outputPaths.inlineUsagePath)
      outputTypealiases.set(outputPaths.typealiasUsagePath)
      outputErrors.set(outputPaths.inlineUsageErrorsPath)
    }

    // Produces a report of packages from included manifests. Null for java-library projects.
    val androidManifestTask = dependencyAnalyzer.registerManifestComponentsExtractionTask()

    // Produces a report that lists all dependencies that contribute Android resources. Null for java-library projects.
    val findAndroidResTask = dependencyAnalyzer.registerFindAndroidResTask()

    // Produces a report of all AAR dependencies with bundled native libs.
    val findNativeLibsTask = dependencyAnalyzer.registerFindNativeLibsTask()

    // A report of service loaders.
    val findServiceLoadersTask = tasks.register<FindServiceLoadersTask>("serviceLoader$taskNameSuffix") {
      setCompileClasspath(
        configurations[dependencyAnalyzer.compileConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      output.set(outputPaths.serviceLoaderDependenciesPath)
    }

    // A report of declared annotation processors.
    val declaredProcsTask = dependencyAnalyzer.registerFindDeclaredProcsTask()

    val synthesizeDependenciesTask =
      tasks.register<SynthesizeDependenciesTask>("synthesizeDependencies$taskNameSuffix") {
        inMemoryCache.set(InMemoryCache.register(project))
        projectPath.set(thisProjectPath)
        compileDependencies.set(graphViewTask.flatMap { it.outputNodes })
        physicalArtifacts.set(artifactsReport.flatMap { it.output })
        explodedJars.set(explodeJarTask.flatMap { it.output })
        inlineMembers.set(kotlinMagicTask.flatMap { it.outputInlineMembers })
        typealiases.set(kotlinMagicTask.flatMap { it.outputTypealiases })
        serviceLoaders.set(findServiceLoadersTask.flatMap { it.output })
        annotationProcessors.set(declaredProcsTask.flatMap { it.output })
        // Optional Android-only inputs
        androidManifestTask?.let { task -> manifestComponents.set(task.flatMap { it.output }) }
        findAndroidResTask?.let { task -> androidRes.set(task.flatMap { it.output }) }
        findNativeLibsTask?.let { task -> nativeLibs.set(task.flatMap { it.output }) }
        findAndroidAssetsTask?.let { task -> androidAssets.set(task.flatMap { it.output }) }

        outputDir.set(outputPaths.dependenciesDir)
      }

    /* ******************************
     * Consumer. Start with introspection: what can we say about this project itself? There are several elements:
     * 1. Source code analysis (the only way to see types used as generic types).
     * 2. Bytecode analysis -- all classes used by our class files.
     * 3. Bytecode analysis -- all classes exposed as the ABI.
     * 4. Android resource analysis -- look for class references and Android resource symbols and IDs.
     *
     * And then synthesize the above.
     ********************************/

    // Lists all import declarations in the source of the current project.
    val explodeCodeSourceTask = tasks.register<CodeSourceExploderTask>("explodeCodeSource$taskNameSuffix") {
      groovySourceFiles.setFrom(dependencyAnalyzer.groovySourceFiles)
      dependencyAnalyzer.javaSourceFiles?.let { javaSourceFiles.setFrom(it) }
      kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
      scalaSourceFiles.setFrom(dependencyAnalyzer.scalaSourceFiles)
      output.set(outputPaths.explodedSourcePath)
    }

    // Lists all classes _used by_ the given project. Analyzes bytecode and collects all class references.
    val explodeBytecodeTask = dependencyAnalyzer.registerByteCodeSourceExploderTask()

    // Lists all possibly-external XML resources referenced by this project's Android resources (or null if this isn't
    // an Android project).
    val explodeXmlSourceTask = dependencyAnalyzer.registerExplodeXmlSourceTask()

    // List all assets provided by this library (or null if this isn't an Android project).
    val explodeAssetSourceTask = dependencyAnalyzer.registerExplodeAssetSourceTask()

    // Describes the project's binary API, or ABI. Null for application projects.
    val abiAnalysisTask = dependencyAnalyzer.registerAbiAnalysisTask(provider {
      // lazy ABI JSON
      with(getExtension().abiHandler.exclusionsHandler) {
        AbiExclusions(
          annotationExclusions = annotationExclusions.get(),
          classExclusions = classExclusions.get(),
          pathExclusions = pathExclusions.get()
        ).toJson()
      }
    })

    val usagesExclusionsProvider = provider {
      with(getExtension().usagesHandler.exclusionsHandler) {
        UsagesExclusions(
          classExclusions = classExclusions.get(),
        ).toJson()
      }
    }

    // Synthesizes the above into a single view of this project's usages.
    val synthesizeProjectViewTask = tasks.register<SynthesizeProjectViewTask>("synthesizeProjectView$taskNameSuffix") {
      projectPath.set(thisProjectPath)
      buildType.set(dependencyAnalyzer.buildType)
      flavor.set(dependencyAnalyzer.flavorName)
      variant.set(variantName)
      kind.set(dependencyAnalyzer.kind)
      graph.set(graphViewTask.flatMap { it.output })
      annotationProcessors.set(declaredProcsTask.flatMap { it.output })
      explodedBytecode.set(explodeBytecodeTask.flatMap { it.output })
      explodedSourceCode.set(explodeCodeSourceTask.flatMap { it.output })
      usagesExclusions.set(usagesExclusionsProvider)
      // Optional: only exists for libraries.
      abiAnalysisTask?.let { t -> explodingAbi.set(t.flatMap { it.output }) }
      // Optional: only exists for Android libraries.
      explodeXmlSourceTask?.let { t -> androidResSource.set(t.flatMap { it.output }) }
      // Optional: only exists for Android libraries.
      explodeAssetSourceTask?.let { t -> androidAssetsSource.set(t.flatMap { it.output }) }
      // Optional: only exists for Android projects.
      dependencyAnalyzer.testInstrumentationRunner?.let { testInstrumentationRunner.set(it) }
      output.set(outputPaths.syntheticProjectPath)
    }

    /* **************************************
     * Producers -> Consumer. Bring it all together. How does this project (consumer) use its dependencies (producers)?
     ****************************************/

    // Computes how this project really uses its dependencies, without consideration for user reporting preferences.
    val computeUsagesTask = tasks.register<ComputeUsagesTask>("computeActualUsage$taskNameSuffix") {
      graph.set(graphViewTask.flatMap { it.output })
      declarations.set(findDeclarationsTask.flatMap { it.output })
      dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      kapt.set(isKaptApplied())
      output.set(outputPaths.dependencyTraceReportPath)
    }

    // Null for JVM projects
    val androidScoreTask = dependencyAnalyzer.registerAndroidScoreTask(
      synthesizeDependenciesTask, synthesizeProjectViewTask
    )

    computeAdviceTask.configure {
      buildPath.set(buildPath(dependencyAnalyzer.compileConfigurationName))
      dependencyGraphViews.add(graphViewTask.flatMap { it.output })
      dependencyUsageReports.add(computeUsagesTask.flatMap { it.output })
      androidScoreTask?.let { t -> androidScoreReports.add(t.flatMap { it.output }) }
    }
  }

  private fun Project.configureAggregationTasks() {
    if (aggregatorsRegistered.getAndSet(true)) return

    val project = this
    val theProjectPath = path
    val paths = NoVariantOutputPaths(this)

    findDeclarationsTask = tasks.register<FindDeclarationsTask>("findDeclarations") {
      FindDeclarationsTask.configureTask(
        task = this,
        project = project,
        outputPaths = paths
      )
    }
    computeAdviceTask = tasks.register<ComputeAdviceTask>("computeAdvice") {
      projectPath.set(theProjectPath)
      declarations.set(findDeclarationsTask.flatMap { it.output })
      bundles.set(getExtension().dependenciesHandler.serializableBundles())
      supportedSourceSets.set(supportedSourceSetNames())
      ignoreKtx.set(getExtension().issueHandler.ignoreKtxFor(theProjectPath))
      ignoreKtx2.set(getExtension().dependenciesHandler.ignoreKtx)
      kapt.set(isKaptApplied())

      output.set(paths.unfilteredAdvicePath)
      dependencyUsages.set(paths.dependencyUsagesPath)
      annotationProcessorUsages.set(paths.annotationProcessorUsagesPath)
      bundledTraces.set(paths.bundledTracesPath)
    }

    val filterAdviceTask = tasks.register<FilterAdviceTask>("filterAdvice") {
      // This information...
      projectAdvice.set(computeAdviceTask.flatMap { it.output })

      // ...is filtered by these preferences...
      dataBindingEnabled.set(isDataBindingEnabled)
      viewBindingEnabled.set(isViewBindingEnabled)
      with(getExtension().issueHandler) {
        // These all have sourceSet-specific behaviors
        anyBehavior.addAll(anyIssueFor(theProjectPath))
        unusedDependenciesBehavior.addAll(unusedDependenciesIssueFor(theProjectPath))
        usedTransitiveDependenciesBehavior.addAll(usedTransitiveDependenciesIssueFor(theProjectPath))
        incorrectConfigurationBehavior.addAll(incorrectConfigurationIssueFor(theProjectPath))
        compileOnlyBehavior.addAll(compileOnlyIssueFor(theProjectPath))
        runtimeOnlyBehavior.addAll(runtimeOnlyIssueFor(theProjectPath))
        unusedProcsBehavior.addAll(unusedAnnotationProcessorsIssueFor(theProjectPath))

        // These don't have sourceSet-specific behaviors
        redundantPluginsBehavior.set(redundantPluginsIssueFor(theProjectPath))
        moduleStructureBehavior.set(moduleStructureIssueFor(theProjectPath))
      }

      // ...and produces this output.
      output.set(paths.filteredAdvicePath)
    }

    val generateProjectHealthReport = tasks.register<GenerateProjectHealthReportTask>("generateConsoleReport") {
      projectAdvice.set(filterAdviceTask.flatMap { it.output })
      dslKind.set(DslKind.from(buildFile))
      dependencyMap.set(getExtension().dependenciesHandler.map)
      output.set(paths.consoleReportPath)
    }

    tasks.register<ProjectHealthTask>("projectHealth") {
      projectAdvice.set(filterAdviceTask.flatMap { it.output })
      consoleReport.set(generateProjectHealthReport.flatMap { it.output })
    }

    reasonTask = tasks.register<ReasonTask>("reason") {
      projectPath.set(theProjectPath)
      dependencyMap.set(getExtension().dependenciesHandler.map)
      dependencyUsageReport.set(computeAdviceTask.flatMap { it.dependencyUsages })
      annotationProcessorUsageReport.set(computeAdviceTask.flatMap { it.annotationProcessorUsages })
      unfilteredAdviceReport.set(computeAdviceTask.flatMap { it.output })
      finalAdviceReport.set(filterAdviceTask.flatMap { it.output })
      bundleTracesReport.set(computeAdviceTask.flatMap { it.bundledTraces })
    }

    tasks.register<RewriteTask>("fixDependencies") {
      buildScript.set(buildFile)
      projectAdvice.set(filterAdviceTask.flatMap { it.output })
      dependencyMap.set(getExtension().dependenciesHandler.map)
    }

    computeResolvedDependenciesTask = tasks.register<ComputeResolvedDependenciesTask>("computeResolvedDependencies") {
      output.set(paths.resolvedDepsPath)
    }

    /*
     * Finalizing work.
     */

    // Store the main output in the extension for consumption by end-users
    storeAdviceOutput(filterAdviceTask.flatMap { it.output })

    // Publish our artifacts, and add project dependencies on root project to this project
    projectHealthPublisher.publish(filterAdviceTask.flatMap { it.output })
    resolvedDependenciesPublisher.publish(computeResolvedDependenciesTask.flatMap { it.output })

    // TODO(tsr): this is cross-project configuration and violates isolated projects (but not CC).
    //  would prefer to do this at the root, but need to validate in the situation when not every subproject applies
    //  DAGP.
    rootProject.dependencies {
      add(projectHealthPublisher.declarableName, project(path))
      add(resolvedDependenciesPublisher.declarableName, project(path))
    }
  }

  /** Get the buildPath of the current build from the root component of the resolution result. */
  private fun Project.buildPath(configuration: String) = configurations[configuration].incoming.resolutionResult.let {
    if (isAtLeastGradle82) {
      it.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
    } else {
      project.provider { @Suppress("DEPRECATION") (it.root.id as ProjectComponentIdentifier).build.name }
    }
  }

  private fun Project.isKaptApplied() = providers.provider { plugins.hasPlugin("org.jetbrains.kotlin.kapt") }

  /**
   * Returns the names of the 'source sets' that are currently supported by the plugin.
   * Dependencies defined on configurations that do not belong to any of these source sets are ignored.
   */
  private fun Project.supportedSourceSetNames() = provider {
    if (pluginManager.hasPlugin(ANDROID_APP_PLUGIN)) {
      the<AppExtension>().applicationVariants.flatMapToSet { sourceSetsForVariant(it) }
    } else if (pluginManager.hasPlugin(ANDROID_LIBRARY_PLUGIN)) {
      the<LibraryExtension>().libraryVariants.flatMapToSet { sourceSetsForVariant(it) }
    } else {
      // JVM Plugins - support all source sets
      the<SourceSetContainer>().matching { s ->
        shouldAnalyzeSourceSetForProject(s.name, project.path)
      }.map { it.name }
    }
  }

  private fun <T> Project.sourceSetsForVariant(variant: T): Set<String> where T : BaseVariant, T : TestedVariant {
    val shouldAnalyzeTests = shouldAnalyzeTests()

    val mainSources = variant.sourceSets.mapToSet { sourceSet -> sourceSet.name }
    val unitTestSources = if (shouldAnalyzeTests) {
      variant.unitTestVariant?.sourceSets?.mapToSet { sourceSet -> sourceSet.name } ?: emptySet()
    } else {
      emptySet()
    }
    val androidTestSources = if (shouldAnalyzeTests) {
      variant.testVariant?.sourceSets?.mapToSet { sourceSet -> sourceSet.name } ?: emptySet()
    } else {
      emptySet()
    }

    return mainSources + unitTestSources + androidTestSources
  }

  private fun SourceSet.jvmSourceSetKind() = when (name) {
    SourceSet.MAIN_SOURCE_SET_NAME -> SourceSetKind.MAIN
    SourceSet.TEST_SOURCE_SET_NAME -> SourceSetKind.TEST
    else -> SourceSetKind.CUSTOM_JVM
  }

  /** Stores advice output in either root extension or subproject extension. */
  private fun Project.storeAdviceOutput(advice: Provider<RegularFile>) {
    if (this == rootProject) {
      getExtension().storeAdviceOutput(advice)
    } else {
      subExtension!!.storeAdviceOutput(advice)
    }
  }

  private class JavaSources(project: Project) {

    val sourceSets: NamedDomainObjectSet<SourceSet> = project.the<SourceSetContainer>().matching { s ->
      project.shouldAnalyzeSourceSetForProject(s.name, project.path)
    }

    val hasJava: Provider<Boolean> = project.provider { sourceSets.flatMap { it.java() }.isNotEmpty() }
  }

  // TODO source set abstractions aren't really working out here.
  private class KotlinSources(project: Project) {

    private val sourceSetContainer = project.the<SourceSetContainer>()
    private val kotlinSourceSets = project.the<KotlinProjectExtension>().sourceSets

    val sourceSets: NamedDomainObjectSet<SourceSet> = sourceSetContainer.matching { s ->
      project.shouldAnalyzeSourceSetForProject(s.name, project.path)
    }

    val hasKotlin: Provider<Boolean> = project.provider { kotlinSourceSets.flatMap { it.kotlin() }.isNotEmpty() }
  }
}

private fun Project.shouldAnalyzeSourceSetForProject(sourceSetName: String, projectPath: String): Boolean {
  return project.getExtension().issueHandler.shouldAnalyzeSourceSet(sourceSetName, projectPath)
    && (project.shouldAnalyzeTests() || sourceSetName != SourceSet.TEST_SOURCE_SET_NAME)
}
