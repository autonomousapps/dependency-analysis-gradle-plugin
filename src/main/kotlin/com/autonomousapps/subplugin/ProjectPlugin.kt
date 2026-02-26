// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.subplugin

import com.android.build.api.artifact.Artifacts
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.*
import com.autonomousapps.AbstractExtension
import com.autonomousapps.DependencyAnalysisExtension
import com.autonomousapps.DependencyAnalysisSubExtension
import com.autonomousapps.Flags.androidIgnoredVariants
import com.autonomousapps.Flags.checkBinaryCompat
import com.autonomousapps.Flags.projectPathRegex
import com.autonomousapps.Flags.shouldAnalyzeTests
import com.autonomousapps.artifacts.Publisher.Companion.interProjectPublisher
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.UsagesExclusions
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.analyzer.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.artifacts.DagpArtifacts
import com.autonomousapps.internal.utils.addAll
import com.autonomousapps.internal.utils.log
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.model.source.AndroidSourceKind
import com.autonomousapps.model.source.JvmSourceKind
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.services.GlobalDslService
import com.autonomousapps.tasks.*
import org.gradle.api.*
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.concurrent.atomic.AtomicBoolean

private const val APPLICATION_PLUGIN = "application"
private const val JAVA_LIBRARY_PLUGIN = "java-library"
private const val JAVA_PLUGIN = "java"
private const val JIB_PLUGIN = "com.google.cloud.tools.jib"
private const val SCALA_PLUGIN = "scala"

private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val ANDROID_TEST_PLUGIN = "com.android.test"

/**
 * @see <a href="https://developer.android.com/kotlin/multiplatform/plugin">Set up the Android Gradle Library Plugin for KMP</a>
 */
private const val ANDROID_KMP_LIBRARY_PLUGIN = "com.android.kotlin.multiplatform.library"
private const val KOTLIN_ANDROID_PLUGIN = "org.jetbrains.kotlin.android"
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"
private const val KOTLIN_MULTIPLATFORM_PLUGIN = "org.jetbrains.kotlin.multiplatform"

private const val GRETTY_PLUGIN = "org.gretty"
private const val SPRING_BOOT_PLUGIN = "org.springframework.boot"

/** This "plugin" is applied to every project in a build. */
internal class ProjectPlugin(private val project: Project) {

  private val dagpExtension: AbstractExtension = if (project == project.rootProject) {
    project.extensions.getByType(DependencyAnalysisExtension::class.java)
  } else {
    DependencyAnalysisSubExtension.of(project)
  }

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

  private lateinit var computeAdviceTask: TaskProvider<ComputeAdviceTask>
  private lateinit var filterAdviceTask: TaskProvider<FilterAdviceTask>
  private lateinit var computeResolvedDependenciesTask: TaskProvider<ComputeResolvedDependenciesTask>
  private lateinit var findDeclarationsTask: TaskProvider<FindDeclarationsTask>
  private lateinit var mergeProjectGraphsTask: TaskProvider<MergeProjectGraphsTask>
  private lateinit var reasonTask: TaskProvider<ReasonTask>
  private lateinit var resolveExternalDependenciesTask: TaskProvider<Task>

  private lateinit var redundantJvmPlugin: RedundantJvmPlugin

  private val isDataBindingEnabled = project.objects.property(Boolean::class.java).convention(false)
  private val isViewBindingEnabled = project.objects.property(Boolean::class.java).convention(false)

  private var isAndroidProject = false
  private var isKmpProject = false

  private val projectHealthPublisher = interProjectPublisher(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PROJECT_HEALTH,
  )
  private val projectMetadataPublisher = interProjectPublisher(
    project = project,
    artifactDescription = DagpArtifacts.Kind.PROJECT_METADATA,
  )
  private val resolvedDependenciesPublisher = interProjectPublisher(
    project = project,
    artifactDescription = DagpArtifacts.Kind.RESOLVED_DEPS,
  )
  private val combinedGraphPublisher = interProjectPublisher(
    project = project,
    artifactDescription = DagpArtifacts.Kind.COMBINED_GRAPH,
  )

  private val dslService = GlobalDslService.of(project)

  fun apply() = project.run {
    // Conditionally disable analysis on some projects
    val projectPathRegex = projectPathRegex()
    if (!projectPathRegex.matches(path)) {
      logger.info("Skipping dependency analysis of project '$path'. Does not match regex '$projectPathRegex'.")
      return
    }

    // Hydrate dependencies map with version catalog entries
    dslService.get().withVersionCatalogs(this)

    maybeConfigureExcludes()

    // Android plugins cannot be wrapped in afterEvaluate because of strict lifecycle checks around access to AGP DSL
    // objects.
    pluginManager.withPlugin(ANDROID_APP_PLUGIN) {
      logger.log("Adding Android application tasks to $path")
      isAndroidProject = true
      checkAgpOnClasspath()
      configureAndroidAppProject()
    }
    pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN) {
      logger.log("Adding Android library tasks to $path")
      isAndroidProject = true
      checkAgpOnClasspath()
      configureAndroidLibProject()
    }
    pluginManager.withPlugin(ANDROID_TEST_PLUGIN) {
      logger.log("Adding Android test tasks to $path")
      isAndroidProject = true
      checkAgpOnClasspath()
      configureAndroidTestProject()
    }

    // In general, we have to configure AGP modules OUTSIDE of an afterEvaluate. However, this plugin is not sufficient.
    // The Android-KMP-LIB plugin also requires the KMP plugin to be applied.
    pluginManager.withPlugin(ANDROID_KMP_LIBRARY_PLUGIN) {
      pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN) {
        logger.log("Adding Android library KMP tasks to $path")

        // Yes, even though this is an Android library. The KMP part is more critical from an analysis perspective. I
        // think.
        isKmpProject = true

        checkAgpOnClasspath()
        checkKgpOnClasspath()

        // Apparently we can reuse the infrastructure for KMP analysis.
        configureKotlinMultiplatformProject()
      }
    }

    // Giving up. Wrap the whole thing in afterEvaluate for simplicity and for access to user configuration via
    // extension.
    afterEvaluate {
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
        checkKgpOnClasspath()
        configureKotlinJvmProject()
      }
      pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN) {
        // Since this is in an afterEvaluate, we hopefully can rely on all plugins being applied, so we check if the AGP
        // plugin is applied. If it's not, proceed with KMP analysis.
        if (!plugins.hasPlugin(ANDROID_KMP_LIBRARY_PLUGIN)) {
          logger.log("Adding Kotlin Multiplatform tasks to ${project.path}")
          isKmpProject = true
          checkKgpOnClasspath()
          configureKotlinMultiplatformProject()
        }
      }
      pluginManager.withPlugin(JAVA_PLUGIN) {
        configureJavaAppProject(maybeAppProject = true)
      }
    }
  }

  /**
   * Certain plugins expect certain dependencies to be available in a way that limits user ability to change. So, we
   * configure DAGP to exclude those dependencies from health reports.
   */
  private fun Project.maybeConfigureExcludes() {
    /*
     * TODO(tsr): user control over the Kotlin stdlib is of a different nature than other dependencies. KGP will add
     *  this automatically to every `api`-like configuration, unless users add `kotlin.stdlib.default.dependency=false`
     *  to their `gradle.properties` file. As such, advice regarding this dependency needs to be handled with more care.
     *  Deal with this in a follow-up. Some kind of DSL opt-in or opt-out.
     */

    val handleKotlin: Action<AppliedPlugin> = Action {
      dagpExtension.issueHandler.project(path) { handler ->
        handler.onAny { issue ->
          issue.exclude("org.jetbrains.kotlin:kotlin-stdlib")
        }
        handler.onRuntimeOnly { issue ->
          // kotlin-reflect must be on the compile classpath: https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1384
          issue.exclude("org.jetbrains.kotlin:kotlin-reflect")
        }
      }
    }

    // If it's a Kotlin project, users have limited ability to make changes to the stdlib.
    pluginManager.withPlugin(KOTLIN_JVM_PLUGIN, handleKotlin)
    pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN, handleKotlin)
    pluginManager.withPlugin(KOTLIN_ANDROID_PLUGIN, handleKotlin)

    // If it's a Scala project, it needs the scala-library dependency.
    pluginManager.withPlugin(SCALA_PLUGIN) {
      dagpExtension.issueHandler.project(path) { handler ->
        handler.onUnusedDependencies { issue ->
          issue.exclude("org.scala-lang:scala-library")
        }
      }
    }
  }

  private fun checkAgpOnClasspath() {
    try {
      @Suppress("UnusedVariable", "unused")
      val a = AndroidComponentsExtension::class.java
    } catch (_: Throwable) {
      dslService.get().notifyAgpMissing()
    }
  }

  private fun checkKgpOnClasspath() {
    try {
      @Suppress("UnusedVariable", "unused")
      val k = KotlinProjectExtension::class.java
    } catch (_: Throwable) {
      dslService.get().notifyKgpMissing()
    }
  }

  /** Has the `com.android.application` plugin applied. */
  private fun Project.configureAndroidAppProject() {
    val project = this
    val ignoredVariantNames = androidIgnoredVariants()

    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
    // val newAgpVersion = androidComponents.pluginVersion.toString().removePrefix("Android Gradle Plugin version ")
    val agpVersion = AgpVersion.current().version

    androidComponents.onVariants { variant ->
      if (variant.name !in ignoredVariantNames) {
        val mainSourceSets = variant.sources

        val unitTest = if (shouldAnalyzeTests() && variant is HasUnitTest) {
          (variant as HasUnitTest).unitTest
        } else {
          null
        }
        val unitTestSourceSets = unitTest?.sources

        val androidTest = if (shouldAnalyzeTests() && variant is HasAndroidTest) {
          variant.androidTest
        } else {
          null
        }
        val androidTestSourceSets = androidTest?.sources

        mainSourceSets.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.main(variant.name),
            variant = variant,
            agpArtifacts = variant.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        unitTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.test(variant.name),
            variant = variant,
            agpArtifacts = unitTest.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        variant.testFixturesSources?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.testFixtures(variant.name),
            variant = variant,
            agpArtifacts = (variant as HasTestFixtures).testFixtures!!.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        androidTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.androidTest(variant.name),
            variant = variant,
            agpArtifacts = androidTest.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = this@configureAndroidAppProject,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }
      }
    }
  }

  /** Has the `com.android.library` plugin applied. */
  private fun Project.configureAndroidLibProject() {
    val project = this
    val ignoredVariantNames = androidIgnoredVariants()

    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
    // val newAgpVersion = androidComponents.pluginVersion.toString().removePrefix("Android Gradle Plugin version ")
    val agpVersion = AgpVersion.current().version

    androidComponents.onVariants { variant ->
      if (variant.name !in ignoredVariantNames) {
        val mainSourceSets = variant.sources

        val unitTest = if (shouldAnalyzeTests() && variant is HasUnitTest) {
          (variant as HasUnitTest).unitTest
        } else {
          null
        }
        val unitTestSourceSets = unitTest?.sources

        val androidTest = if (shouldAnalyzeTests() && variant is HasAndroidTest) {
          variant.androidTest
        } else {
          null
        }
        val androidTestSourceSets = androidTest?.sources

        mainSourceSets.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.main(variant.name),
            variant = variant,
            agpArtifacts = variant.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
            hasAbi = true,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        variant.testFixturesSources?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.testFixtures(variant.name),
            variant = variant,
            agpArtifacts = (variant as HasTestFixtures).testFixtures!!.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
            hasAbi = true,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        unitTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.test(variant.name),
            variant = variant,
            agpArtifacts = unitTest.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
            hasAbi = false,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        androidTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.androidTest(variant.name),
            variant = variant,
            agpArtifacts = androidTest.artifacts,
            sources = sourceSets,
          )
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
            hasAbi = false,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }
      }
    }
  }

  /** Has the `com.android.test` plugin applied. */
  private fun Project.configureAndroidTestProject() {
    val project = this
    val ignoredVariantNames = androidIgnoredVariants()

    val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
    // val newAgpVersion = androidComponents.pluginVersion.toString().removePrefix("Android Gradle Plugin version ")
    val agpVersion = AgpVersion.current().version

    androidComponents.onVariants { variant ->
      if (variant.name !in ignoredVariantNames) {
        val mainSourceSets = variant.sources

        // nb: com.android.test projects have no test nor androidTest source.

        mainSourceSets.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet(
            sourceKind = AndroidSourceKind.main(variant.name),
            variant = variant,
            agpArtifacts = variant.artifacts,
            sources = sourceSets,
            forComAndroidTestModule = true,
          )
          val dependencyAnalyzer = AndroidTestAnalyzer(
            project = project,
            variant = DefaultAndroidVariant(project, variant),
            agpVersion = agpVersion,
            androidSources = variantSourceSet,
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }
      }
    }
  }

  private fun newVariantSourceSet(
    sourceKind: AndroidSourceKind,
    variant: Variant,
    agpArtifacts: Artifacts,
    sources: Sources,
    /** `com.android.test` modules have special requirements. */
    forComAndroidTestModule: Boolean = false,
  ): AndroidSources {
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1111
    // https://issuetracker.google.com/issues/325307775
    // if ~/.android/analytics.settings has `hasOptedIn` set to `true`, then
    // `./gradlew :app:explodeXmlSourceDebugTest --no-daemon` will fail. This only happens for unit test analysis.
    // Running "AndroidTestDependenciesSpec.transitive test dependencies should be declared on testImplementation*" will
    // reproduce this error. I don't yet know how to set up a test environment that can reproduce that failure
    // hermetically (that is, without having to adjust my user home directory).
    return if (sourceKind.kind == SourceKind.TEST_KIND) {
      TestAndroidSources(
        project = project,
        sources = sources,
        primaryAgpVariant = variant,
        agpArtifacts = agpArtifacts,
        sourceKind = sourceKind,
        compileClasspathConfigurationName = sourceKind.compileClasspathName,
        runtimeClasspathConfigurationName = sourceKind.runtimeClasspathName,
      )
    } else if (forComAndroidTestModule) {
      ComAndroidTestAndroidSources(
        project = project,
        sources = sources,
        primaryAgpVariant = variant,
        agpArtifacts = agpArtifacts,
        sourceKind = sourceKind,
        compileClasspathConfigurationName = sourceKind.compileClasspathName,
        runtimeClasspathConfigurationName = sourceKind.runtimeClasspathName,
      )
    } else {
      DefaultAndroidSources(
        project = project,
        sources = sources,
        primaryAgpVariant = variant,
        agpArtifacts = agpArtifacts,
        sourceKind = sourceKind,
        compileClasspathConfigurationName = sourceKind.compileClasspathName,
        runtimeClasspathConfigurationName = sourceKind.runtimeClasspathName,
      )
    }
  }

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

      val j = JavaSources(this, dagpExtension)
      j.sourceSets.forEach { sourceSet ->
        try {
          analyzeDependencies(
            JavaWithoutAbiAnalyzer(
              project = this,
              sourceSet = sourceSet,
              sourceKind = JvmSourceKind.of(sourceSet.name),
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
    val j = JavaSources(this, dagpExtension)

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

    if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
      logger.warn(
        "(dependency analysis) You have both java-library and org.springframework.boot applied. You probably want java, not java-library."
      )
    }

    j.sourceSets.forEach { sourceSet ->
      try {
        val sourceKind = JvmSourceKind.of(sourceSet.name)
        val hasAbi = hasAbi(sourceSet)
        val dependencyAnalyzer = if (hasAbi) {
          JavaWithAbiAnalyzer(
            project = this,
            sourceSet = sourceSet,
            sourceKind = sourceKind,
            hasAbi = true,
          )
        } else {
          JavaWithoutAbiAnalyzer(
            project = this,
            sourceSet = sourceSet,
            sourceKind = sourceKind,
          )
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
    val k = KotlinSources(this, dagpExtension)

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
        val sourceKind = JvmSourceKind.of(sourceSet.name)
        val hasAbi = hasAbi(sourceSet)

        val dependencyAnalyzer = if (hasAbi) {
          KotlinJvmLibAnalyzer(
            project = this,
            sourceSet = sourceSet,
            sourceKind = sourceKind,
            hasAbi = true,
          )
        } else {
          KotlinJvmAppAnalyzer(
            project = this,
            sourceSet = sourceSet,
            sourceKind = sourceKind,
          )
        }

        analyzeDependencies(dependencyAnalyzer)
      } catch (_: UnknownTaskException) {
        logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
      }
    }
  }

  /**
   * Has the [KOTLIN_MULTIPLATFORM_PLUGIN] plugin applied, and optionally the [ANDROID_KMP_LIBRARY_PLUGIN] plugin as
   * well.
   */
  private fun Project.configureKotlinMultiplatformProject() {
    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)

    kotlin
      .targets
      .configureEach { target ->
//        println("CONFIGURING KOTLIN TARGET ${target.name} (${target.javaClass.simpleName})")
        target.compilations
          // TODO(tsr): requiring the `runtimeDependencyConfigurationName` to be non-null seems to be equivalent to only
          //  configuring the JVM and Android targets.
          //  Not sure yet how to handle non-JVM/Android targets.
          //  nb: there are no "common" targets. Those source sets must be incorporated into the actual targets as file
          //      collections.
          .matching { compilation -> compilation.runtimeDependencyConfigurationName != null }
          .configureEach { compilation ->
//            println(" COMPILATION ${compilation.name} (${compilation.javaClass.simpleName})")
            try {
              val hasAbi = hasAbi(compilation)
              val kmpSourceSet = KmpSourceSet(compilation)

              val dependencyAnalyzer = KmpProjectAnalyzer(
                project = this,
                sourceSet = kmpSourceSet,
                hasAbi = hasAbi,
              )

              analyzeDependencies(dependencyAnalyzer)
            } catch (_: UnknownTaskException) {
              logger.warn("Skipping tasks creation for KMP target `${target.name}`")
            }
          }
      }
  }

  private fun Project.hasAbi(sourceSet: SourceSet): Boolean {
    if (sourceSet.name in dagpExtension.abiHandler.exclusionsHandler.excludedSourceSets.get()) {
      // if this sourceSet is user-excluded, then it doesn't have an ABI
      return false
    }

    val hasApiConfiguration = configurations.findByName(sourceSet.apiConfigurationName) != null
    // The 'test' sourceSet does not have an ABI
    val isNotTest = sourceSet.name != SourceSet.TEST_SOURCE_SET_NAME
    // The 'main' sourceSet for an app project does not have an ABI
    val isNotMainApp = !(isAppProject() && sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME)
    return hasApiConfiguration && isNotTest && isNotMainApp
  }

  private fun Project.hasAbi(compilation: KotlinCompilation<*>): Boolean {
    val sourceSetName = compilation.defaultSourceSet.name
    if (sourceSetName in dagpExtension.abiHandler.exclusionsHandler.excludedSourceSets.get()) {
      // if this sourceSet is user-excluded, then it doesn't have an ABI
      return false
    }

    val hasApiConfiguration = configurations.named(compilation.apiConfigurationName) != null
    // 'xTest' sourceSets do not have an ABI (this is a heuristic)
    val isNotTest = !sourceSetName.endsWith("Test")
    // The 'main' sourceSet for an app project does not have an ABI
    val isNotMainApp = !(isAppProject() && sourceSetName == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)
    return hasApiConfiguration && isNotTest && isNotMainApp
  }

  private fun Project.isAppProject(): Boolean {
    return dagpExtension.forceAppProject
      || pluginManager.hasPlugin(ANDROID_APP_PLUGIN)
      || pluginManager.hasPlugin(APPLICATION_PLUGIN)
      || pluginManager.hasPlugin(GRETTY_PLUGIN)
      || pluginManager.hasPlugin(JIB_PLUGIN)
      || pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)
  }

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
        redundantPluginsBehavior = dagpExtension.issueHandler.redundantPluginsIssueFor(projectPath)
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

    /*
     * Metadata about the dependency graph.
     */

    // Lists the dependencies declared for building the project, along with their physical artifacts (jars).
    val artifactsReport = dependencyAnalyzer.registerArtifactsReportForCompileTask()

    // Lists the dependencies declared for running the project, along with their physical artifacts (jars).
    val artifactsReportRuntime = dependencyAnalyzer.registerArtifactsReportForRuntimeTask()

    // Produce a DAG of the compile and runtime classpaths rooted on this project.
    val graphViewTask = dependencyAnalyzer.registerGraphViewTask(findDeclarationsTask)

    reasonTask.configure { t ->
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.output /* compile graph */ })
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.outputRuntime })
    }

    /*
     * Optional utility tasks (not part of buildHealth). Here because they can easily utilize DAGP's infrastructure.
     */

    val resolveExternalDependencies = dependencyAnalyzer.registerResolveExternalDependenciesTask()

    // Lifecycle tasks to resolve ALL external dependencies for ALL source sets.
    resolveExternalDependenciesTask.configure { t ->
      t.dependsOn(resolveExternalDependencies)
    }

    computeResolvedDependenciesTask.configure { t ->
      t.externalDependencies.add(resolveExternalDependencies.flatMap { it.output })
    }

    dependencyAnalyzer.registerDominatorTreeTasks(
      artifactsReportCompile = artifactsReport,
      artifactsReportRuntime = artifactsReportRuntime,
      graphViewTask = graphViewTask,
    )

    dependencyAnalyzer.registerGenerateProjectGraphTasks(mergeProjectGraphsTask)

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
    val explodeJarTask = dependencyAnalyzer.registerExplodeJarTask(artifactsReport, androidLintTask)

    // Find the inline members of this project's dependencies.
    val kotlinMagicTask = dependencyAnalyzer.registerFindKotlinMagicTask(artifactsReport)

    // Produces a report of packages from included manifests. Null for java-library projects.
    val androidManifestTask = dependencyAnalyzer.registerManifestComponentsExtractionTask()

    // Produces a report that lists all dependencies that contribute Android resources. Null for java-library projects.
    val findAndroidResTask = dependencyAnalyzer.registerFindAndroidResTask()

    // Produces a report of all JAR or AAR dependencies with bundled native libs (.so or .dylib).
    val findNativeLibsTask = dependencyAnalyzer.registerFindNativeLibsTask()

    // A report of service loaders.
    val findServiceLoadersTask = dependencyAnalyzer.registerFindServiceLoadersTask()

    // A report of declared annotation processors.
    val declaredProcsTask = dependencyAnalyzer.registerFindDeclaredProcsTask()

    // Re-synthesize dependencies from analysis.
    val synthesizeDependenciesTask = dependencyAnalyzer.registerSynthesizeDependenciesTask(
      graphViewTask,
      artifactsReport,
      explodeJarTask,
      kotlinMagicTask,
      findServiceLoadersTask,
      declaredProcsTask,
      findNativeLibsTask,
      androidManifestTask,
      findAndroidResTask,
      findAndroidAssetsTask,
    )

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
    val explodeCodeSourceTask = dependencyAnalyzer.registerCodeSourceExploderTask()

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
      with(dagpExtension.abiHandler.exclusionsHandler) {
        AbiExclusions(
          annotationInclusions = annotationInclusions.get(),
          classInclusions = classInclusions.get(),
          annotationExclusions = annotationExclusions.get(),
          classExclusions = classExclusions.get(),
          pathExclusions = pathExclusions.get()
        ).toJson()
      }
    })

    val usagesExclusionsProvider = provider {
      with(dagpExtension.usageHandler.exclusionsHandler) {
        UsagesExclusions(
          classExclusions = classExclusions.get(),
        ).toJson()
      }
    }

    // Synthesizes the above into a single view of this project's usages.
    val synthesizeProjectViewTask = dependencyAnalyzer.registerSynthesizeProjectViewTask(
      graphViewTask,
      declaredProcsTask,
      explodeBytecodeTask,
      explodeCodeSourceTask,
      usagesExclusionsProvider,
      artifactsReport,
      abiAnalysisTask,
      explodeXmlSourceTask,
      explodeAssetSourceTask,
    )

    // Discover duplicates on compile and runtime classpaths
    val duplicateClassesCompile =
      dependencyAnalyzer.registerDiscoverClasspathDuplicationForCompileTask(synthesizeProjectViewTask)
    val duplicateClassesRuntime =
      dependencyAnalyzer.registerDiscoverClasspathDuplicationForRuntimeTask(synthesizeProjectViewTask)

    computeAdviceTask.configure { t ->
      t.duplicateClassesReports.add(duplicateClassesCompile.flatMap { it.output })
      t.duplicateClassesReports.add(duplicateClassesRuntime.flatMap { it.output })
    }

    /* **************************************
     * Producers -> Consumer. Bring it all together. How does this project (consumer) use its dependencies (producers)?
     ****************************************/

    // Computes how this project really uses its dependencies, without consideration for user reporting preferences.
    val computeUsagesTask = dependencyAnalyzer.registerComputeUsagesTask(
      checkSuperClasses = dagpExtension.usageHandler.analysisHandler.checkSuperClasses,
      checkBinaryCompat = checkBinaryCompat(),
      isKaptApplied = isKaptApplied(),
      graphViewTask = graphViewTask,
      findDeclarationsTask = findDeclarationsTask,
      synthesizeProjectViewTask = synthesizeProjectViewTask,
      synthesizeDependenciesTask = synthesizeDependenciesTask,
      duplicateClassesCompile = duplicateClassesCompile,
      duplicateClassesRuntime = duplicateClassesRuntime,
    )

    // Null for JVM projects
    val androidScoreTask = dependencyAnalyzer.registerAndroidScoreTask(
      synthesizeDependenciesTask,
      synthesizeProjectViewTask,
    )

    computeAdviceTask.configure { t ->
      t.buildPath.set(buildPath(dependencyAnalyzer.compileConfigurationName))
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.output })
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.outputRuntime })
      t.dependencyUsageReports.add(computeUsagesTask.flatMap { it.output })
      androidScoreTask?.let { a -> t.androidScoreReports.add(a.flatMap { it.output }) }
    }
    filterAdviceTask.configure { t ->
      t.buildPath.set(buildPath(dependencyAnalyzer.compileConfigurationName))
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.output })
      t.dependencyGraphViews.add(graphViewTask.flatMap { it.outputRuntime })
    }
  }

  private fun Project.configureAggregationTasks() {
    if (aggregatorsRegistered.getAndSet(true)) return

    val project = this
    val theProjectPath = path
    val projectType = ProjectType.of(isAndroidProject = isAndroidProject, isKmpProject = isKmpProject)
    val supportedSourceSetNames = supportedSourceSetNames()
    val paths = NoVariantOutputPaths(this)

    findDeclarationsTask = tasks.register("findDeclarations", FindDeclarationsTask::class.java) { t ->
      FindDeclarationsTask.configureTask(
        task = t,
        project = project,
        projectType = projectType,
        supportedSourceSetNames = supportedSourceSetNames,
        outputPaths = paths
      )
    }
    computeAdviceTask = tasks.register("computeAdvice", ComputeAdviceTask::class.java) { t ->
      t.projectPath.set(theProjectPath)
      t.declarations.set(findDeclarationsTask.flatMap { it.output })
      t.bundles.set(dagpExtension.dependenciesHandler.serializableBundles())
      t.supportedSourceSets.set(supportedSourceSetNames)
      t.ignoreKtx.set(dagpExtension.dependenciesHandler.ignoreKtx)
      t.explicitSourceSets.set(dagpExtension.dependenciesHandler.explicitSourceSets)
      t.projectType.set(projectType)
      t.kapt.set(isKaptApplied())

      t.output.set(paths.unfilteredAdvicePath)
      t.dependencyUsages.set(paths.dependencyUsagesPath)
      t.annotationProcessorUsages.set(paths.annotationProcessorUsagesPath)
      t.bundledTraces.set(paths.bundledTracesPath)
    }

    filterAdviceTask = tasks.register("filterAdvice", FilterAdviceTask::class.java) { t ->
      // This information...
      t.projectAdvice.set(computeAdviceTask.flatMap { it.output })

      // ...is filtered by these preferences...
      t.dataBindingEnabled.set(isDataBindingEnabled)
      t.viewBindingEnabled.set(isViewBindingEnabled)
      with(dagpExtension.issueHandler) {
        // These all have sourceSet-specific behaviors
        t.anyBehavior.addAll(anyIssueFor(theProjectPath))
        t.unusedDependenciesBehavior.addAll(unusedDependenciesIssueFor(theProjectPath))
        t.usedTransitiveDependenciesBehavior.addAll(usedTransitiveDependenciesIssueFor(theProjectPath))
        t.incorrectConfigurationBehavior.addAll(incorrectConfigurationIssueFor(theProjectPath))
        t.compileOnlyBehavior.addAll(compileOnlyIssueFor(theProjectPath))
        t.runtimeOnlyBehavior.addAll(runtimeOnlyIssueFor(theProjectPath))
        t.unusedProcsBehavior.addAll(unusedAnnotationProcessorsIssueFor(theProjectPath))
        t.duplicateClassWarningsBehavior.addAll(onDuplicateClassWarnings(theProjectPath))

        // These don't have sourceSet-specific behaviors
        t.redundantPluginsBehavior.set(redundantPluginsIssueFor(theProjectPath))
        t.moduleStructureBehavior.set(moduleStructureIssueFor(theProjectPath))
      }

      // ...and produces this output.
      t.output.set(paths.filteredAdvicePath)
    }

    val writeProjectMetadata = tasks.register("writeProjectMetadata", WriteProjectMetadataTask::class.java) { t ->
      t.projectPath.set(theProjectPath)
      t.projectType.set(projectType)
      t.output.set(paths.projectMetadataPath)
    }

    val generateProjectHealthReport =
      tasks.register("generateConsoleReport", GenerateProjectHealthReportTask::class.java) { t ->
        t.projectAdvice.set(filterAdviceTask.flatMap { it.output })
        t.projectMetadata.set(writeProjectMetadata.flatMap { it.output })
        t.reportingConfig.set(dagpExtension.reportingHandler.config())
        t.dslKind.set(DslKind.from(buildFile))
        t.dependencyMap.set(dagpExtension.dependenciesHandler.map)
        t.useTypesafeProjectAccessors.set(dagpExtension.useTypesafeProjectAccessors)
        t.useParenthesesForGroovy.set(dagpExtension.dependenciesHandler.useParenthesesForGroovy)
        t.output.set(paths.consoleReportPath)
      }

    tasks.register("projectHealth", ProjectHealthTask::class.java) { t ->
      t.buildFilePath.set(project.buildFile.path)
      t.projectAdvice.set(filterAdviceTask.flatMap { it.output })
      t.consoleReport.set(generateProjectHealthReport.flatMap { it.output })
    }

    reasonTask = tasks.register("reason", ReasonTask::class.java) { t ->
      t.projectPath.set(theProjectPath)
      t.buildPath.set(buildPath(buildscript.configurations.named("classpath")))
      t.dependencyMap.set(dagpExtension.dependenciesHandler.map)
      t.dependencyUsageReport.set(computeAdviceTask.flatMap { it.dependencyUsages })
      t.annotationProcessorUsageReport.set(computeAdviceTask.flatMap { it.annotationProcessorUsages })
      t.unfilteredAdviceReport.set(computeAdviceTask.flatMap { it.output })
      t.finalAdviceReport.set(filterAdviceTask.flatMap { it.output })
      t.bundleTracesReport.set(computeAdviceTask.flatMap { it.bundledTraces })
    }

    tasks.register("fixDependencies", RewriteTask::class.java) { t ->
      t.buildScript.set(buildFile)
      t.projectAdvice.set(filterAdviceTask.flatMap { it.output })
      t.projectMetadata.set(writeProjectMetadata.flatMap { it.output })
      t.dependencyMap.set(dagpExtension.dependenciesHandler.map)
      t.useTypesafeProjectAccessors.set(dagpExtension.useTypesafeProjectAccessors)
      t.useParenthesesForGroovy.set(dagpExtension.dependenciesHandler.useParenthesesForGroovy)
    }

    resolveExternalDependenciesTask = tasks.register("resolveExternalDependencies")

    computeResolvedDependenciesTask =
      tasks.register("computeResolvedDependencies", ComputeResolvedDependenciesTask::class.java) { t ->
        t.output.set(paths.resolvedDepsPath)
        t.outputToml.set(paths.resolvedAllLibsVersionsTomlPath)
      }

    mergeProjectGraphsTask = tasks.register("generateMergedProjectGraph", MergeProjectGraphsTask::class.java) { t ->
      t.output.set(paths.mergedProjectGraphPath)
    }

    /*
     * Finalizing work.
     */

    // Store the main output in the extension for consumption by end-users
    storeAdviceOutput(filterAdviceTask.flatMap { it.output })

    // Publish our artifacts
    combinedGraphPublisher.publish(mergeProjectGraphsTask.flatMap { it.output })
    projectHealthPublisher.publish(filterAdviceTask.flatMap { it.output })
    projectMetadataPublisher.publish(writeProjectMetadata.flatMap { it.output })
    resolvedDependenciesPublisher.publish(computeResolvedDependenciesTask.flatMap { it.output })
  }

  private fun Project.isKaptApplied() = providers.provider { plugins.hasPlugin("org.jetbrains.kotlin.kapt") }

  /**
   * Returns the names of the 'source sets' that are currently supported by the plugin. Dependencies defined on
   * configurations that do not belong to any of these source sets are ignored.
   */
  private fun Project.supportedSourceSetNames(): Provider<Set<String>> {
    return provider {
      when {
        appliesAndroidPlugin() -> {
          extensions.getByType(CommonExtension::class.java)
            .sourceSets
            .matching { s -> shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path) }
            .map { it.name }
        }

        appliesKotlinMultiplatformPlugin() -> {
          project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            .sourceSets
            .matching { s -> shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path) }
            .map { it.name }
        }

        // JVM Plugins
        else -> {
          project.extensions.getByType(SourceSetContainer::class.java)
            .matching { s -> shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path) }
            .map { it.name }
        }
      }.toSortedSet()
    }
  }

  private val Variant.testFixturesSources: Sources?
    get() = if (this is HasTestFixtures && testFixtures != null) {
      testFixtures!!.sources
    } else {
      null
    }

  private fun Project.appliesAndroidPlugin(): Boolean =
    pluginManager.hasPlugin(ANDROID_APP_PLUGIN)
      || pluginManager.hasPlugin(ANDROID_LIBRARY_PLUGIN)
      || pluginManager.hasPlugin(ANDROID_TEST_PLUGIN)

  private fun Project.appliesKotlinMultiplatformPlugin(): Boolean = pluginManager.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN)

  /** Stores advice output in either root extension or subproject extension. */
  private fun storeAdviceOutput(advice: Provider<RegularFile>) {
    dagpExtension.storeAdviceOutput(advice)
  }

  private class JavaSources(project: Project, dagpExtension: AbstractExtension) {

    val sourceSets: NamedDomainObjectSet<SourceSet> =
      project.extensions.getByType(SourceSetContainer::class.java).matching { s ->
        project.shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path)
      }

    val hasJava: Provider<Boolean> = project.provider { sourceSets.flatMap { it.java() }.isNotEmpty() }
  }

  // TODO source set abstractions aren't really working out here.
  private class KotlinSources(project: Project, dagpExtension: AbstractExtension) {

    private val sourceSetContainer = project.extensions.getByType(SourceSetContainer::class.java)
    private val _kotlinSourceSets = project.extensions.getByType(KotlinProjectExtension::class.java).sourceSets

    val sourceSets: NamedDomainObjectSet<SourceSet> = sourceSetContainer.matching { s ->
      project.shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path)
    }

    val kotlinSourceSets: NamedDomainObjectSet<KotlinSourceSet> = _kotlinSourceSets.matching { s ->
      project.shouldAnalyzeSourceSetForProject(dagpExtension, s.name, project.path)
    }

    val hasKotlin: Provider<Boolean> = project.provider { _kotlinSourceSets.flatMap { it.kotlin() }.isNotEmpty() }
  }
}

private fun Project.shouldAnalyzeSourceSetForProject(
  dagpExtension: AbstractExtension,
  sourceSetName: String,
  projectPath: String,
): Boolean {
  val analysisEnabledForSourceSet = dagpExtension.issueHandler.shouldAnalyzeSourceSet(sourceSetName, projectPath)
  val isTestSourceSet = when (sourceSetName) {
    SourceSet.TEST_SOURCE_SET_NAME -> true
    SourceKind.ANDROID_TEST_NAME -> true
    else -> false
  }
  return analysisEnabledForSourceSet && (project.shouldAnalyzeTests() || !isTestSourceSet)
}
