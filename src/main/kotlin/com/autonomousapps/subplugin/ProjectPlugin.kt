package com.autonomousapps.subplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.builder.model.SourceProvider
import com.autonomousapps.*
import com.autonomousapps.internal.AbiExclusions
import com.autonomousapps.internal.NoVariantOutputPaths
import com.autonomousapps.internal.UsagesExclusions
import com.autonomousapps.internal.analyzer.*
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.configuration.Configurations
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.log
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.concurrent.atomic.AtomicBoolean

private const val BASE_PLUGIN = "base"
private const val ANDROID_APP_PLUGIN = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN = "com.android.library"
private const val APPLICATION_PLUGIN = "application"
private const val JAVA_LIBRARY_PLUGIN = "java-library"
private const val JAVA_PLUGIN = "java"
private const val SPRING_BOOT_PLUGIN = "org.springframework.boot"

/** This plugin can be applied along with java-library, so needs special care */
private const val KOTLIN_JVM_PLUGIN = "org.jetbrains.kotlin.jvm"

/**
 * This "plugin" is applied to every project in a build.
 */
internal class ProjectPlugin(private val project: Project) {

  companion object {
    private val JAVA_COMPARATOR by lazy {
      Comparator<SourceProvider> { s1, s2 -> s1.name.compareTo(s2.name) }
    }
    private val KOTLIN_COMPARATOR by lazy {
      Comparator<KotlinSourceSet> { s1, s2 -> s1.name.compareTo(s2.name) }
    }
  }

  private val projectPath = project.path

  private lateinit var inMemoryCacheProvider: Provider<InMemoryCache>

  /**
   * Used by non-root projects.
   */
  private var subExtension: DependencyAnalysisSubExtension? = null

  /**
   * Used as a gate to prevent this plugin from configuring a project more than once. If ever
   * checked and the value is already `true`, creates and configures the [RedundantPlugin].
   */
  private val configuredForKotlinJvmOrJavaLibrary = AtomicBoolean(false)

  /**
   * Used as a gate to prevent this plugin from configuring an app project more than once. This has
   * been added because we now react to the plain ol' `java` plugin, in order to be able to analyze
   * Spring Boot projects. However, both the `application` and `java-library` plugins also apply
   * `java`, so we have to prevent double-configuration.
   */
  private val configuredForJavaProject = AtomicBoolean(false)

  private lateinit var findDeclarationsTask: TaskProvider<FindDeclarationsTask>
  private lateinit var redundantPlugin: RedundantPlugin
  private lateinit var computeAdviceTask: TaskProvider<ComputeAdviceTask>
  private val isDataBindingEnabled = project.objects.property<Boolean>().convention(false)
  private val isViewBindingEnabled = project.objects.property<Boolean>().convention(false)

  fun apply() = project.run {
    inMemoryCacheProvider = InMemoryCache.register(gradle)
    createSubExtension()

    val outputPaths = NoVariantOutputPaths(this)
    findDeclarationsTask = tasks.register<FindDeclarationsTask>("findDeclarations") {
      FindDeclarationsTask.configureTask(
        task = this,
        project = this@run,
        outputPaths = outputPaths
      )
    }
    computeAdviceTask = tasks.register<ComputeAdviceTask>("computeAdvice") {
      projectPath.set(this@ProjectPlugin.projectPath)
    }

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
      maybeConfigureSpringBootProject()
    }

    addAggregationTasks()
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
    // We need the afterEvaluate so we can get a reference to the `KotlinCompile` tasks. This is due to use of the
    // pluginManager.withPlugin API. Currently configuring the com.android.application plugin, not any Kotlin plugin.
    // I do not know how to wait for both plugins to be ready.
    afterEvaluate {
      // If kotlin-android is applied, get the Kotlin source sets
      val kotlinSourceSets = findKotlinSourceSets()

      val appExtension = the<AppExtension>()
      appExtension.applicationVariants.all {
        val mainSourceSets = sourceSets
        val unitTestSourceSets = if (shouldAnalyzeTests()) unitTestVariant?.sourceSets else null

        mainSourceSets.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet2(name, SourceSetKind.MAIN, sourceSets, kotlinSourceSets)
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = this@configureAndroidAppProject,
            variant = this,
            agpVersion = AgpVersion.current().version,
            variantSourceSet = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        unitTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet2(name, SourceSetKind.TEST, sourceSets, kotlinSourceSets)
          val dependencyAnalyzer = AndroidAppAnalyzer(
            project = this@configureAndroidAppProject,
            variant = this,
            agpVersion = AgpVersion.current().version,
            variantSourceSet = variantSourceSet
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
    afterEvaluate {
      // If kotlin-android is applied, get the Kotlin source sets
      val kotlinSourceSets = findKotlinSourceSets()

      the<LibraryExtension>().libraryVariants.all {
        val mainSourceSets = sourceSets
        val unitTestSourceSets = if (shouldAnalyzeTests()) unitTestVariant?.sourceSets else null

        mainSourceSets.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet2(name, SourceSetKind.MAIN, sourceSets, kotlinSourceSets)
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = this@configureAndroidLibProject,
            variant = this,
            agpVersion = AgpVersion.current().version,
            variantSourceSet = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }

        unitTestSourceSets?.let { sourceSets ->
          val variantSourceSet = newVariantSourceSet2(name, SourceSetKind.TEST, sourceSets, kotlinSourceSets)
          val dependencyAnalyzer = AndroidLibAnalyzer(
            project = this@configureAndroidLibProject,
            variant = this,
            agpVersion = AgpVersion.current().version,
            variantSourceSet = variantSourceSet
          )
          isDataBindingEnabled.set(dependencyAnalyzer.isDataBindingEnabled)
          isViewBindingEnabled.set(dependencyAnalyzer.isViewBindingEnabled)
          analyzeDependencies(dependencyAnalyzer)
        }
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

  private fun newVariantSourceSet2(
    variantName: String,
    kind: SourceSetKind,
    androidSourceSets: List<SourceProvider>,
    kotlinSourceSets: NamedDomainObjectContainer<KotlinSourceSet>?
  ): VariantSourceSet {
    return VariantSourceSet(
      kind = kind,
      androidSourceSets = androidSourceSets.toSortedSet(JAVA_COMPARATOR),
      kotlinSourceSets = kotlinSourceSets?.filterToOrderedSet(KOTLIN_COMPARATOR) { k ->
        androidSourceSets.any { a -> a.name == k.name }
      },
      compileClasspathConfigurationName = kind.compileClasspathConfigurationName(variantName)
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

  private fun Project.maybeConfigureSpringBootProject() {
    configureJavaAppProject(maybeSpringBoot = true)
  }

  /**
   * Has the `application` plugin applied. The `org.jetbrains.kotlin.jvm` may or may not be applied.
   * If it is applied, this is a kotlin-jvm-app project. If it isn't, a java-jvm-app project.
   */
  private fun Project.configureJavaAppProject(maybeSpringBoot: Boolean = false) {
    afterEvaluate {
      if (maybeSpringBoot) {
        if (!pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
          // This means we only discovered the java plugin, which isn't sufficient
          return@afterEvaluate
        }
        logger.log("Adding JVM tasks to ${project.path}")
      }

      // If kotlin-jvm is NOT applied, then go ahead and configure the project as a java-jvm-app
      // project. If it IS applied, do nothing. We will configure this as a kotlin-jvm-app project
      // in `configureKotlinJvmProject()`.
      if (!pluginManager.hasPlugin(KOTLIN_JVM_PLUGIN)) {
        if (configuredForJavaProject.getAndSet(true)) {
          logger.info("(dependency analysis) $path was already configured")
          return@afterEvaluate
        }

        val sourceSets = the<SourceSetContainer>()
        val testSource = if (shouldAnalyzeTests()) sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME) else null
        val mainSource = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
        mainSource?.let { sourceSet ->
          try {
            analyzeDependencies(
              JavaAppAnalyzer(
                project = this,
                sourceSet = sourceSet,
                testSourceSet = testSource,
                kind = SourceSetKind.MAIN
              )
            )
          } catch (_: UnknownTaskException) {
            logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
          }
        }

        testSource?.let { sourceSet ->
          try {
            analyzeDependencies(
              JavaAppAnalyzer(
                project = this,
                sourceSet = sourceSet,
                testSourceSet = null,
                kind = SourceSetKind.TEST
              )
            )
          } catch (_: UnknownTaskException) {
            logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
          }
        }
      }
    }
  }

  /**
   * Has the `java-library` plugin applied.
   */
  private fun Project.configureJavaLibProject() {
    afterEvaluate {
      val sourceSets = the<SourceSetContainer>()

      val javaFiles = sourceSets.flatMap {
        it.java.sourceDirectories.asFileTree.matching {
          include("**/*.java")
        }
      }
      val hasJava = providers.provider { javaFiles.isNotEmpty() }

      configureRedundantPlugin2 {
        it.withJava(hasJava)
      }

      if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
        logger.info("(dependency analysis) $path was already configured for the kotlin-jvm plugin")
        redundantPlugin.configure()
        return@afterEvaluate
      }

      if (configuredForJavaProject.getAndSet(true)) {
        logger.info("(dependency analysis) $path was already configured")
        return@afterEvaluate
      }

      val testSource = if (shouldAnalyzeTests()) sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME) else null
      val mainSource = sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
      mainSource?.let { sourceSet ->
        try {
          // Regardless of the fact that this is a "java-library" project, the presence of Spring
          // Boot indicates an app project.
          val dependencyAnalyzer = if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
            logger.warn(
              "(dependency analysis) You have both java-library and org.springframework.boot applied. You probably " +
                "want java, not java-library."
            )
            JavaAppAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = testSource,
              kind = SourceSetKind.MAIN
            )
          } else {
            JavaLibAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = testSource,
              kind = SourceSetKind.MAIN,
              hasAbi = true
            )
          }
          analyzeDependencies(dependencyAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
        }
      }

      testSource?.let { sourceSet ->
        try {
          // Regardless of the fact that this is a "java-library" project, the presence of Spring
          // Boot indicates an app project.
          val dependencyAnalyzer = if (pluginManager.hasPlugin(SPRING_BOOT_PLUGIN)) {
            JavaAppAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = null,
              kind = SourceSetKind.TEST
            )
          } else {
            JavaLibAnalyzer(
              project = this,
              sourceSet = sourceSet,
              testSourceSet = null,
              kind = SourceSetKind.TEST,
              hasAbi = false
            )
          }
          analyzeDependencies(dependencyAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
        }
      }
    }
  }

  /**
   * Has the `org.jetbrains.kotlin.jvm` (aka `kotlin("jvm")`) plugin applied. The `application` (and
   * by implication the `java`) plugin may or may not be applied. If it is, this is an app project.
   * If it isn't, this is a library project.
   */
  private fun Project.configureKotlinJvmProject() {
    afterEvaluate {
      val kotlin = the<KotlinProjectExtension>()

      val kotlinFiles = kotlin.sourceSets
        .flatMap {
          it.kotlin.sourceDirectories.asFileTree.matching {
            include("**/*.kt")
          }
        }
      val hasKotlin = provider { kotlinFiles.isNotEmpty() }

      configureRedundantPlugin2 {
        it.withKotlin(hasKotlin)
      }

      if (configuredForKotlinJvmOrJavaLibrary.getAndSet(true)) {
        logger.info("(dependency analysis) $path was already configured for the java-library plugin")
        redundantPlugin.configure()
        return@afterEvaluate
      }

      val mainSource = kotlin.sourceSets.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
      val testSource = if (shouldAnalyzeTests()) kotlin.sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME) else null
      mainSource?.let { mainSourceSet ->
        try {
          val dependencyAnalyzer =
            if (isAppProject()) {
              KotlinJvmAppAnalyzer(
                project = this,
                sourceSet = mainSourceSet,
                testSourceSet = testSource,
                kind = SourceSetKind.MAIN
              )
            } else {
              KotlinJvmLibAnalyzer(
                project = this,
                mainSourceSet = mainSourceSet,
                testSourceSet = testSource,
                kind = SourceSetKind.MAIN,
                hasAbi = true
              )
            }
          analyzeDependencies(dependencyAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${mainSourceSet.name}`")
        }
      }

      testSource?.let { sourceSet ->
        try {
          val dependencyAnalyzer =
            if (isAppProject()) {
              KotlinJvmAppAnalyzer(
                project = this,
                sourceSet = sourceSet,
                testSourceSet = null,
                kind = SourceSetKind.TEST
              )
            } else {
              KotlinJvmLibAnalyzer(
                project = this,
                mainSourceSet = sourceSet,
                testSourceSet = null,
                kind = SourceSetKind.TEST,
                hasAbi = false
              )
            }
          analyzeDependencies(dependencyAnalyzer)
        } catch (_: UnknownTaskException) {
          logger.warn("Skipping tasks creation for sourceSet `${sourceSet.name}`")
        }
      }
    }
  }

  private fun Project.isAppProject() =
    pluginManager.hasPlugin(APPLICATION_PLUGIN) ||
      pluginManager.hasPlugin(SPRING_BOOT_PLUGIN) ||
      pluginManager.hasPlugin(ANDROID_APP_PLUGIN)

  /* ===============================================
   * The main work of the plugin happens below here.
   * ===============================================
   */

  private fun Project.configureRedundantPlugin2(block: (RedundantPlugin) -> Unit) {
    if (!::redundantPlugin.isInitialized) {
      redundantPlugin = RedundantPlugin(
        project = this,
        computeAdviceTask = computeAdviceTask,
        redundantPluginsBehavior = getExtension().issueHandler.redundantPluginsIssue()
      )
    }

    block(redundantPlugin)
  }

  /**
   * Subproject tasks are registered here. This function is called in a loop, once for each Android variant & source
   * set, or Java source set.
   */
  private fun Project.analyzeDependencies(dependencyAnalyzer: DependencyAnalyzer) {
    val thisProjectPath = path
    val variantName = dependencyAnalyzer.variantName
    val taskNameSuffix = dependencyAnalyzer.taskNameSuffix
    val outputPaths = dependencyAnalyzer.outputPaths

    /*
     * Metadata about the dependency graph.
     */

    // Lists the dependencies required to build the project, along with their physical artifacts (jars).
    val artifactsReportTask = tasks.register<ArtifactsReportTask>("artifactsReport$taskNameSuffix") {
      setCompileClasspath(
        configurations[dependencyAnalyzer.compileConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )

      output.set(outputPaths.artifactsPath)
      outputPretty.set(outputPaths.artifactsPrettyPath)
    }

    // Produce a DAG of the compile classpath rooted on this project.
    val graphViewTask = tasks.register<GraphViewTask>("graphView$taskNameSuffix") {
      setCompileClasspath(configurations[dependencyAnalyzer.compileConfigurationName])
      jarAttr.set(dependencyAnalyzer.attributeValueJar)
      projectPath.set(thisProjectPath)
      variant.set(variantName)
      kind.set(dependencyAnalyzer.kind)
      output.set(outputPaths.compileGraphPath)
      outputDot.set(outputPaths.compileGraphDotPath)
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

    // Explode jars to expose their secrets.
    val explodeJarTask = tasks.register<ExplodeJarTask>("explodeJar$taskNameSuffix") {
      inMemoryCache.set(inMemoryCacheProvider)
      setCompileClasspath(
        configurations[dependencyAnalyzer.compileConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
      androidLintTask?.let { task ->
        androidLinters.set(task.flatMap { it.output })
      }

      output.set(outputPaths.allDeclaredDepsPath)
      outputPretty.set(outputPaths.allDeclaredDepsPrettyPath)
    }

    // Find the inline members of this project's dependencies.
    val inlineTask = tasks.register<FindInlineMembersTask>("findInlineMembers$taskNameSuffix") {
      inMemoryCacheProvider.set(this@ProjectPlugin.inMemoryCacheProvider)
      setCompileClasspath(
        configurations[dependencyAnalyzer.compileConfigurationName].artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      artifacts.set(artifactsReportTask.flatMap { it.output })
      output.set(outputPaths.inlineUsagePath)
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
        configurations[dependencyAnalyzer.compileConfigurationName]
          .artifactsFor(dependencyAnalyzer.attributeValueJar)
      )
      output.set(outputPaths.serviceLoaderDependenciesPath)
    }

    // A report of declared annotation processors.
    val declaredProcsTask = dependencyAnalyzer.registerFindDeclaredProcsTask(inMemoryCacheProvider)

    val synthesizeDependenciesTask =
      tasks.register<SynthesizeDependenciesTask>("synthesizeDependencies$taskNameSuffix") {
        inMemoryCache.set(inMemoryCacheProvider)
        projectPath.set(thisProjectPath)
        graphView.set(graphViewTask.flatMap { it.output })
        physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
        explodedJars.set(explodeJarTask.flatMap { it.output })
        inlineMembers.set(inlineTask.flatMap { it.output })
        serviceLoaders.set(findServiceLoadersTask.flatMap { it.output })
        annotationProcessors.set(declaredProcsTask.flatMap { it.output })
        // Optional Android-only inputs
        androidManifestTask?.let { task -> manifestComponents.set(task.flatMap { it.output }) }
        findAndroidResTask?.let { task -> androidRes.set(task.flatMap { it.output }) }
        findNativeLibsTask?.let { task -> nativeLibs.set(task.flatMap { it.output }) }

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
      dependencyAnalyzer.javaSourceFiles?.let { javaSourceFiles.setFrom(it) }
      kotlinSourceFiles.setFrom(dependencyAnalyzer.kotlinSourceFiles)
      output.set(outputPaths.explodedSourcePath)
    }

    // Lists all classes _used by_ the given project. Analyzes bytecode and collects all class references.
    val explodeBytecodeTask = dependencyAnalyzer.registerByteCodeSourceExploderTask()

    // Lists all possibly-external XML resources referenced by this project's Android resources (or null if this isn't
    // an Android project).
    val explodeXmlSourceTask = dependencyAnalyzer.registerExplodeXmlSourceTask()

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
      output.set(outputPaths.syntheticProject)
    }

    /* **************************************
     * Producers -> Consumer. Bring it all together. How does this project (consumer) use its dependencies (producers)?
     ****************************************/

    // Computes how this project really uses its dependencies, without consideration for user reporting preferences.
    val computeUsagesTask = tasks.register<ComputeUsagesTask>("computeActualUsage$taskNameSuffix") {
      graph.set(graphViewTask.flatMap { it.output })
      locations.set(findDeclarationsTask.flatMap { it.output })
      dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      output.set(outputPaths.usagesPath)
    }

    computeAdviceTask.configure {
      dependencyGraphViews.add(graphViewTask.flatMap { it.output })
      dependencyUsageReports.add(computeUsagesTask.flatMap { it.output })
    }
  }

  private fun Project.addAggregationTasks() {
    val projectPath = path
    val paths = NoVariantOutputPaths(this)

    computeAdviceTask.configure {
      locations.set(findDeclarationsTask.flatMap { it.output })
      bundles.set(getExtension().dependenciesHandler.serializableBundles())
      ignoreKtx.set(getExtension().issueHandler.ignoreKtxFor(projectPath))
      kapt.set(providers.provider { plugins.hasPlugin("kotlin-kapt") })
      output.set(paths.unfilteredAdvicePath)
    }

    val filterAdviceTask = tasks.register<FilterAdviceTask>("filterAdvice") {
      // This information...
      projectAdvice.set(computeAdviceTask.flatMap { it.output })

      // ...is filtered by these preferences...
      dataBindingEnabled.set(isDataBindingEnabled)
      viewBindingEnabled.set(isViewBindingEnabled)
      with(getExtension().issueHandler) {
        anyBehavior.set(anyIssueFor(projectPath))
        unusedDependenciesBehavior.set(unusedDependenciesIssueFor(projectPath))
        usedTransitiveDependenciesBehavior.set(usedTransitiveDependenciesIssueFor(projectPath))
        incorrectConfigurationBehavior.set(incorrectConfigurationIssueFor(projectPath))
        compileOnlyBehavior.set(compileOnlyIssueFor(projectPath))
        unusedProcsBehavior.set(unusedAnnotationProcessorsIssueFor(projectPath))
        redundantPluginsBehavior.set(redundantPluginsIssueFor(projectPath))
      }

      // ...and produces this output.
      output.set(paths.filteredAdvicePath)
    }

    val generateProjectHealthReport = tasks.register<GenerateProjectHealthReportTask>("generateConsoleReport") {
      projectAdvice.set(filterAdviceTask.flatMap { it.output })
      output.set(paths.consoleReportPath)
    }

    tasks.register<ProjectHealthTask>("projectHealth") {
      projectAdvice.set(filterAdviceTask.flatMap { it.output })
      consoleReport.set(generateProjectHealthReport.flatMap { it.output })
    }

    // Store the main output in the extension for consumption by end-users
    storeAdviceOutput(filterAdviceTask.flatMap { it.output })

    publishArtifact(
      producerConfName = Configurations.CONF_ADVICE_ALL_PRODUCER,
      consumerConfName = Configurations.CONF_ADVICE_ALL_CONSUMER,
      output = filterAdviceTask.flatMap { it.output }
    )

    // Remove the above artifact from the `archives` configuration (to which it is automagically
    // added), and which led to the task that produced it being made a dependency of `assemble`,
    // which led to undesirable behavior. See also https://github.com/gradle/gradle/issues/10797.
    pluginManager.withPlugin(BASE_PLUGIN) {
      if (shouldClearArtifacts()) {
        configurations["archives"].artifacts.clear()
      }
    }
  }

  /**
   * Publishes an artifact for consumption by the root project.
   */
  private fun Project.publishArtifact(
    producerConfName: String,
    consumerConfName: String,
    output: Provider<RegularFile>
  ) {
    // outgoing configurations, containers for our project reports for the root project to consume
    val conf = configurations.create(producerConfName) {
      isCanBeResolved = false
      isCanBeConsumed = true

      outgoing.artifact(output)
    }

    // Add project dependency on root project to this project, with our new configurations
    rootProject.dependencies {
      add(consumerConfName, project(path, conf.name))
    }
  }

  /**
   * Stores advice output in either root extension or subproject extension.
   */
  private fun Project.storeAdviceOutput(advice: Provider<RegularFile>) {
    if (this == rootProject) {
      getExtension().storeAdviceOutput(advice)
    } else {
      subExtension!!.storeAdviceOutput(advice)
    }
  }
}
