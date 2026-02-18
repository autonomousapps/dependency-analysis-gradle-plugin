// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.model.DuplicateClass
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/** Abstraction for differentiating between android-app, android-lib, and java-lib projects.  */
internal interface DependencyAnalyzer {
  /** E.g., `flavorDebug` */
  val variantName: String

  /** E.g., 'flavor' */
  val flavorName: String?

  /** E.g., 'debug' */
  val buildType: String?

  val sourceKind: SourceKind

  /** E.g., `FlavorDebugTest` */
  val taskNameSuffix: String

  /** E.g., "compileClasspath", "debugCompileClasspath". */
  val compileConfigurationName: String

  /** E.g., "runtimeClasspath", "debugRuntimeClasspath". */
  val runtimeConfigurationName: String

  /** E.g., "kaptDebug" */
  val kaptConfigurationName: String

  /** E.g., "annotationProcessorDebug" */
  val annotationProcessorConfigurationName: String

  /** E.g., "androidx.test.runner.AndroidJUnitRunner" */
  val testInstrumentationRunner: Provider<String>

  val attributeValueJar: String

  val isDataBindingEnabled: Provider<Boolean>
  val isViewBindingEnabled: Provider<Boolean>

  val outputPaths: OutputPaths

  /**
   * This is a no-op for `com.android.application` and JVM `application` projects (including Spring Boot), since they
   * have no meaningful ABI.
   */
  fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? = null

  /** Compute this project's Android Score (lower score means it could be a JVM project). */
  fun registerAndroidScoreTask(
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<AndroidScoreTask>? = null

  /** Lists the dependencies declared for building the project, along with their physical artifacts (jars). */
  fun registerArtifactsReportForCompileTask(): TaskProvider<ArtifactsReportTask>

  /** Lists the dependencies declared for running the project, along with their physical artifacts (jars). */
  fun registerArtifactsReportForRuntimeTask(): TaskProvider<ArtifactsReportTask>

  fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask>
  fun registerCodeSourceExploderTask(): TaskProvider<out CodeSourceExploderTask>

  /**
   * Computes how this project really uses its dependencies, without consideration for user reporting preferences.
   */
  fun registerComputeUsagesTask(
    checkSuperClasses: Provider<Boolean>,
    checkBinaryCompat: Provider<Boolean>,
    isKaptApplied: Provider<Boolean>,
    graphViewTask: TaskProvider<GraphViewTask>,
    findDeclarationsTask: TaskProvider<FindDeclarationsTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    duplicateClassesCompile: TaskProvider<DiscoverClasspathDuplicationTask>,
    duplicateClassesRuntime: TaskProvider<DiscoverClasspathDuplicationTask>,
  ): TaskProvider<ComputeUsagesTask>

  /** Discover duplicates on the compile classpath. */
  fun registerDiscoverClasspathDuplicationForCompileTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask>

  /** Discover duplicates on the runtime classpath. */
  fun registerDiscoverClasspathDuplicationForRuntimeTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask>

  /**
   * Registers optional utility tasks (not part of `buildHealth`). They compute the dominance tree for the compile
   * classpath and the runtime classpath. The tasks are:
   * 1. `computeDominatorTreeCompile<Variant>`
   * 2. `computeDominatorTreeRuntime<Variant>`
   * 3. `computeDominatorTree<Variant>` (lifecycle tasks that depends on `1` and `2`)
   * 4. `printDominatorTreeCompile<Variant>`
   * 5. `printDominatorTreeRuntime<Variant>`
   */
  fun registerDominatorTreeTasks(
    artifactsReportCompile: TaskProvider<ArtifactsReportTask>,
    artifactsReportRuntime: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>,
  )

  /** List all assets provided by this library (or null if this isn't an Android project). */
  fun registerExplodeAssetSourceTask(): TaskProvider<AssetSourceExploderTask>? = null

  /** Explode jars to expose their secrets. */
  fun registerExplodeJarTask(
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    androidLintTask: TaskProvider<FindAndroidLinters>?,
  ): TaskProvider<ExplodeJarTask>

  /**
   * Lists all possibly-external XML resources referenced by this project's Android resources (or null if this isn't an
   * Android project).
   */
  fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask>? = null

  /** A report of all dependencies that supply Android assets on the compile classpath. */
  fun registerFindAndroidAssetProvidersTask(): TaskProvider<FindAndroidAssetProviders>? = null

  /** A report of all dependencies that supply Android linters on the compile classpath. */
  fun registerFindAndroidLintersTask(): TaskProvider<FindAndroidLinters>? = null

  /**
   * Produces a report that lists all dependencies that contribute Android resources. Null for java-library projects.
   */
  fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask>? = null

  /** A report of declared annotation processors. */
  fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask>

  /** Find the inline members of this project's dependencies. */
  fun registerFindKotlinMagicTask(artifactsReport: TaskProvider<ArtifactsReportTask>): TaskProvider<FindKotlinMagicTask>

  /** Produces a report of all JAR or AAR dependencies with bundled native libs (.so or .dylib). */
  fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask>

  /** A report of service loaders. */
  fun registerFindServiceLoadersTask(): TaskProvider<FindServiceLoadersTask>

  /**
   * Registers optional utility tasks (not part of `buildHealth`). These:
   *
   * 1. Generates graph view of local (project) dependencies
   * 2. Prints some help text relating to generateProjectGraphTask. This is the "user-facing" task.
   * 3. Merges the graphs from generateProjectGraphTask into a single variant-agnostic output.
   */
  fun registerGenerateProjectGraphTasks(mergeProjectGraphsTask: TaskProvider<MergeProjectGraphsTask>)

  /** Produce a DAG of the compile and runtime classpaths rooted on this project. */
  fun registerGraphViewTask(findDeclarationsTask: TaskProvider<FindDeclarationsTask>): TaskProvider<GraphViewTask>

  /** Produces a report of packages from included manifests. Null for java-library projects. */
  fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask>? = null

  /**
   * An optional utility task (not part of `buildHealth`). This resolves all external dependencies across compile and
   * runtime configurations.
   */
  fun registerResolveExternalDependenciesTask(): TaskProvider<ResolveExternalDependenciesTask>

  /** Re-synthesize dependencies from analysis. */
  fun registerSynthesizeDependenciesTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    kotlinMagicTask: TaskProvider<FindKotlinMagicTask>,
    findServiceLoadersTask: TaskProvider<FindServiceLoadersTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    findNativeLibsTask: TaskProvider<FindNativeLibsTask>,
    androidManifestTask: TaskProvider<ManifestComponentsExtractionTask>?,
    findAndroidResTask: TaskProvider<FindAndroidResTask>?,
    findAndroidAssetsTask: TaskProvider<FindAndroidAssetProviders>?,
  ): TaskProvider<SynthesizeDependenciesTask>

  /** Synthesizes the above into a single view of this project's usages. */
  fun registerSynthesizeProjectViewTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    explodeBytecodeTask: TaskProvider<ClassListExploderTask>,
    explodeCodeSourceTask: TaskProvider<out CodeSourceExploderTask>,
    usagesExclusionsProvider: Provider<String>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    abiAnalysisTask: TaskProvider<AbiAnalysisTask>?,
    explodeXmlSourceTask: TaskProvider<XmlSourceExploderTask>?,
    explodeAssetSourceTask: TaskProvider<AssetSourceExploderTask>?,
  ): TaskProvider<SynthesizeProjectViewTask>
}

internal abstract class AbstractDependencyAnalyzer(
  protected val project: Project,
) : DependencyAnalyzer {

  // Always null for JVM projects. May be null for Android projects.
  override val testInstrumentationRunner: Provider<String> = project.provider { null }

  final override fun registerArtifactsReportForCompileTask(): TaskProvider<ArtifactsReportTask> {
    return project.tasks.register("artifactsReport$taskNameSuffix", ArtifactsReportTask::class.java) { t ->
      t.setConfiguration(project.configurations.named(compileConfigurationName)) { c ->
        c.artifactsFor(attributeValueJar)
      }
      t.buildPath.set(project.buildPath(compileConfigurationName))

      t.output.set(outputPaths.compileArtifactsPath)
      t.excludedIdentifiersOutput.set(outputPaths.excludedIdentifiersPath)
    }
  }

  final override fun registerArtifactsReportForRuntimeTask(): TaskProvider<ArtifactsReportTask> {
    return project.tasks.register("artifactsReportRuntime$taskNameSuffix", ArtifactsReportTask::class.java) { t ->
      t.setConfiguration(project.configurations.named(runtimeConfigurationName)) { c ->
        c.artifactsFor(attributeValueJar)
      }
      t.buildPath.set(project.buildPath(runtimeConfigurationName))

      t.output.set(outputPaths.runtimeArtifactsPath)
      t.excludedIdentifiersOutput.set(outputPaths.excludedIdentifiersRuntimePath)
    }
  }

  final override fun registerComputeUsagesTask(
    checkSuperClasses: Provider<Boolean>,
    checkBinaryCompat: Provider<Boolean>,
    isKaptApplied: Provider<Boolean>,
    graphViewTask: TaskProvider<GraphViewTask>,
    findDeclarationsTask: TaskProvider<FindDeclarationsTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    duplicateClassesCompile: TaskProvider<DiscoverClasspathDuplicationTask>,
    duplicateClassesRuntime: TaskProvider<DiscoverClasspathDuplicationTask>
  ): TaskProvider<ComputeUsagesTask> {
    return project.tasks.register("computeActualUsage$taskNameSuffix", ComputeUsagesTask::class.java) { t ->
      t.checkSuperClasses.set(checkSuperClasses)
      // Currently only modeling this via Gradle property. TODO(tsr): hoist it to the DSL.
      t.checkBinaryCompat.set(checkBinaryCompat)

      t.graph.set(graphViewTask.flatMap { it.output })
      t.declarations.set(findDeclarationsTask.flatMap { it.output })
      t.dependencies.set(synthesizeDependenciesTask.flatMap { it.outputDir })
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.kapt.set(isKaptApplied)
      t.duplicateClassesReports.add(duplicateClassesCompile.flatMap { it.output })
      t.duplicateClassesReports.add(duplicateClassesRuntime.flatMap { it.output })
      t.output.set(outputPaths.dependencyTraceReportPath)
    }
  }

  final override fun registerDominatorTreeTasks(
    artifactsReportCompile: TaskProvider<ArtifactsReportTask>,
    artifactsReportRuntime: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>,
  ) {
    val computeDominatorCompile = registerComputeDominatorTreeForCompileTask(artifactsReportCompile, graphViewTask)
    val computeDominatorRuntime = registerComputeDominatorTreeForRuntimeTask(artifactsReportRuntime, graphViewTask)

    // a lifecycle task that computes the dominator tree for both compile and runtime classpaths
    project.tasks.register("computeDominatorTree$taskNameSuffix") { t ->
      t.dependsOn(computeDominatorCompile, computeDominatorRuntime)
    }

    project.tasks.register("printDominatorTreeCompile$taskNameSuffix", PrintDominatorTreeTask::class.java) { t ->
      t.consoleText.set(computeDominatorCompile.flatMap { it.outputTxt })
    }

    project.tasks.register("printDominatorTreeRuntime$taskNameSuffix", PrintDominatorTreeTask::class.java) { t ->
      t.consoleText.set(computeDominatorRuntime.flatMap { it.outputTxt })
    }
  }

  private fun registerComputeDominatorTreeForCompileTask(
    artifactsReportTask: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>,
  ): TaskProvider<ComputeDominatorTreeTask> {
    return project.tasks.register(
      "computeDominatorTreeCompile$taskNameSuffix",
      ComputeDominatorTreeTask::class.java
    ) { t ->
      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.projectPath.set(project.path)
      t.physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
      t.graphView.set(graphViewTask.flatMap { it.output })

      t.outputTxt.set(outputPaths.compileDominatorConsolePath)
      t.outputDot.set(outputPaths.compileDominatorGraphPath)
      t.outputJson.set(outputPaths.compileDominatorJsonPath)
    }
  }

  private fun registerComputeDominatorTreeForRuntimeTask(
    artifactsReportTask: TaskProvider<ArtifactsReportTask>,
    graphViewTask: TaskProvider<GraphViewTask>
  ): TaskProvider<ComputeDominatorTreeTask> {
    return project.tasks.register(
      "computeDominatorTreeRuntime$taskNameSuffix",
      ComputeDominatorTreeTask::class.java
    ) { t ->
      t.buildPath.set(project.buildPath(runtimeConfigurationName))
      t.projectPath.set(project.path)
      t.physicalArtifacts.set(artifactsReportTask.flatMap { it.output })
      t.graphView.set(graphViewTask.flatMap { it.outputRuntime })

      t.outputTxt.set(outputPaths.runtimeDominatorConsolePath)
      t.outputDot.set(outputPaths.runtimeDominatorGraphPath)
      t.outputJson.set(outputPaths.runtimeDominatorJsonPath)
    }
  }

  final override fun registerDiscoverClasspathDuplicationForCompileTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask> {
    return project.tasks.register(
      "discoverDuplicationForCompile$taskNameSuffix",
      DiscoverClasspathDuplicationTask::class.java,
    ) { t ->
      t.withClasspathName(DuplicateClass.COMPILE_CLASSPATH_NAME)
      t.setClasspath(
        project.configurations
          .getByName(compileConfigurationName)
          .artifactsFor(attributeValueJar)
      )
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.output.set(outputPaths.duplicateCompileClasspathPath)
    }
  }

  final override fun registerDiscoverClasspathDuplicationForRuntimeTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<DiscoverClasspathDuplicationTask> {
    return project.tasks.register(
      "discoverDuplicationForRuntime$taskNameSuffix",
      DiscoverClasspathDuplicationTask::class.java,
    ) { t ->
      t.withClasspathName(DuplicateClass.RUNTIME_CLASSPATH_NAME)
      t.setClasspath(
        project.configurations
          .getByName(runtimeConfigurationName)
          .artifactsFor(attributeValueJar)
      )
      t.syntheticProject.set(synthesizeProjectViewTask.flatMap { it.output })
      t.output.set(outputPaths.duplicateCompileRuntimePath)
    }
  }

  final override fun registerExplodeJarTask(
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    androidLintTask: TaskProvider<FindAndroidLinters>?,
  ): TaskProvider<ExplodeJarTask> {
    return project.tasks.register("explodeJar$taskNameSuffix", ExplodeJarTask::class.java) { t ->
      t.inMemoryCache.set(InMemoryCache.register(project))
      t.compileClasspath.setFrom(
        project.configurations.getByName(compileConfigurationName)
          .artifactsFor(attributeValueJar)
          .artifactFiles
      )
      t.physicalArtifacts.set(artifactsReport.flatMap { it.output })
      androidLintTask?.let { t2 -> t.androidLinters.set(t2.flatMap { it.output }) }

      t.output.set(outputPaths.explodedJarsPath)
    }
  }

  final override fun registerFindKotlinMagicTask(artifactsReport: TaskProvider<ArtifactsReportTask>): TaskProvider<FindKotlinMagicTask> {
    return project.tasks.register("findKotlinMagic$taskNameSuffix", FindKotlinMagicTask::class.java) { t ->
      t.inMemoryCacheProvider.set(InMemoryCache.register(project))
      t.compileClasspath.setFrom(
        project.configurations.getByName(compileConfigurationName)
          .artifactsFor(attributeValueJar)
          .artifactFiles
      )
      t.artifacts.set(artifactsReport.flatMap { it.output })
      t.outputInlineMembers.set(outputPaths.inlineUsagePath)
      t.outputTypealiases.set(outputPaths.typealiasUsagePath)
      t.outputErrors.set(outputPaths.inlineUsageErrorsPath)
    }
  }

  final override fun registerFindServiceLoadersTask(): TaskProvider<FindServiceLoadersTask> {
    return project.tasks.register("serviceLoader$taskNameSuffix", FindServiceLoadersTask::class.java) { t ->
      // TODO(tsr): consider this. Wouldn't the runtime classpath be more appropriate for this task? Separate PR to test.
      //  it.setCompileClasspath(configurations.getByName(dependencyAnalyzer.runtimeConfigurationName).artifactsFor(dependencyAnalyzer.attributeValueJar))
      t.setCompileClasspath(
        project.configurations
          .getByName(compileConfigurationName)
          .artifactsFor(attributeValueJar)
      )
      t.output.set(outputPaths.serviceLoaderDependenciesPath)
    }
  }

  final override fun registerGenerateProjectGraphTasks(mergeProjectGraphsTask: TaskProvider<MergeProjectGraphsTask>) {
    val generateProjectGraphTask =
      project.tasks.register("generateProjectGraph$taskNameSuffix", GenerateProjectGraphTask::class.java) { t ->
        t.buildPath.set(project.buildPath(compileConfigurationName))

        t.compileClasspath.set(
          project.configurations.getByName(compileConfigurationName)
            .incoming
            .resolutionResult
            .rootComponent
        )
        t.runtimeClasspath.set(
          project.configurations.getByName(runtimeConfigurationName)
            .incoming
            .resolutionResult
            .rootComponent
        )
        t.output.set(outputPaths.projectGraphDir)
      }

    // Prints some help text relating to generateProjectGraphTask. This is the "user-facing" task.
    project.tasks.register("projectGraph$taskNameSuffix", ProjectGraphTask::class.java) { t ->
      t.rootDir.set(project.rootDir)
      t.projectPath.set(project.path)
      t.graphsDir.set(generateProjectGraphTask.flatMap { it.output })
    }

    // Merges the graphs from generateProjectGraphTask into a single variant-agnostic output.
    mergeProjectGraphsTask.configure { t ->
      t.projectGraphs.add(generateProjectGraphTask.flatMap {
        it.output.file(GenerateProjectGraphTask.PROJECT_COMBINED_CLASSPATH_JSON)
      })
    }
  }

  final override fun registerGraphViewTask(findDeclarationsTask: TaskProvider<FindDeclarationsTask>): TaskProvider<GraphViewTask> {
    return project.tasks.register("graphView$taskNameSuffix", GraphViewTask::class.java) { t ->
      t.configureTask(
        project = project,
        compileClasspath = project.configurations.getByName(compileConfigurationName),
        runtimeClasspath = project.configurations.getByName(runtimeConfigurationName),
        jarAttr = attributeValueJar
      )
      t.buildPath.set(project.buildPath(compileConfigurationName))
      t.projectPath.set(project.path)
      t.sourceKind.set(sourceKind)
      t.declarations.set(findDeclarationsTask.flatMap { it.output })

      t.output.set(outputPaths.compileGraphPath)
      t.outputDot.set(outputPaths.compileGraphDotPath)
      t.outputNodes.set(outputPaths.compileNodesPath)
      t.outputRuntime.set(outputPaths.runtimeGraphPath)
      t.outputRuntimeDot.set(outputPaths.runtimeGraphDotPath)
    }
  }

  final override fun registerResolveExternalDependenciesTask(): TaskProvider<ResolveExternalDependenciesTask> {
    return project.tasks.register(
      "resolveExternalDependencies$taskNameSuffix",
      ResolveExternalDependenciesTask::class.java,
    ) { t ->
      t.configureTask(
        project = project,
        compileClasspath = project.configurations.getByName(compileConfigurationName),
        runtimeClasspath = project.configurations.getByName(runtimeConfigurationName),
        jarAttr = attributeValueJar,
      )
      t.output.set(outputPaths.externalDependenciesPath)
    }
  }

  final override fun registerSynthesizeDependenciesTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    kotlinMagicTask: TaskProvider<FindKotlinMagicTask>,
    findServiceLoadersTask: TaskProvider<FindServiceLoadersTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    findNativeLibsTask: TaskProvider<FindNativeLibsTask>,
    androidManifestTask: TaskProvider<ManifestComponentsExtractionTask>?,
    findAndroidResTask: TaskProvider<FindAndroidResTask>?,
    findAndroidAssetsTask: TaskProvider<FindAndroidAssetProviders>?,
  ): TaskProvider<SynthesizeDependenciesTask> {
    return project.tasks.register(
      "synthesizeDependencies$taskNameSuffix",
      SynthesizeDependenciesTask::class.java
    ) { t ->
      t.inMemoryCache.set(InMemoryCache.register(project))
      t.projectPath.set(project.path)
      t.compileDependencies.set(graphViewTask.flatMap { it.outputNodes })
      t.physicalArtifacts.set(artifactsReport.flatMap { it.output })
      t.explodedJars.set(explodeJarTask.flatMap { it.output })
      t.inlineMembers.set(kotlinMagicTask.flatMap { it.outputInlineMembers })
      t.typealiases.set(kotlinMagicTask.flatMap { it.outputTypealiases })
      t.serviceLoaders.set(findServiceLoadersTask.flatMap { it.output })
      t.annotationProcessors.set(declaredProcsTask.flatMap { it.output })
      t.nativeLibs.set(findNativeLibsTask.flatMap { it.output })
      // Optional Android-only inputs
      androidManifestTask?.let { t2 -> t.manifestComponents.set(t2.flatMap { it.output }) }
      findAndroidResTask?.let { t2 -> t.androidRes.set(t2.flatMap { it.output }) }
      findAndroidAssetsTask?.let { t2 -> t.androidAssets.set(t2.flatMap { it.output }) }

      t.outputDir.set(outputPaths.dependenciesDir)
    }
  }

  final override fun registerSynthesizeProjectViewTask(
    graphViewTask: TaskProvider<GraphViewTask>,
    declaredProcsTask: TaskProvider<FindDeclaredProcsTask>,
    explodeBytecodeTask: TaskProvider<ClassListExploderTask>,
    explodeCodeSourceTask: TaskProvider<out CodeSourceExploderTask>,
    usagesExclusionsProvider: Provider<String>,
    artifactsReport: TaskProvider<ArtifactsReportTask>,
    abiAnalysisTask: TaskProvider<AbiAnalysisTask>?,
    explodeXmlSourceTask: TaskProvider<XmlSourceExploderTask>?,
    explodeAssetSourceTask: TaskProvider<AssetSourceExploderTask>?
  ): TaskProvider<SynthesizeProjectViewTask> {
    return project.tasks.register("synthesizeProjectView$taskNameSuffix", SynthesizeProjectViewTask::class.java) { t ->
      t.projectPath.set(project.path)
      t.buildType.set(buildType)
      t.flavor.set(flavorName)
      t.variant.set(variantName)
      t.sourceKind.set(sourceKind)
      t.graph.set(graphViewTask.flatMap { it.output })
      t.annotationProcessors.set(declaredProcsTask.flatMap { it.output })
      t.explodedBytecode.set(explodeBytecodeTask.flatMap { it.output })
      t.explodedSourceCode.set(explodeCodeSourceTask.flatMap { it.output })
      t.usagesExclusions.set(usagesExclusionsProvider)
      t.excludedIdentifiers.set(artifactsReport.flatMap { it.excludedIdentifiersOutput })
      // Optional: only exists for libraries.
      abiAnalysisTask?.let { t2 -> t.explodingAbi.set(t2.flatMap { it.output }) }
      // Optional: only exists for Android libraries.
      explodeXmlSourceTask?.let { t2 ->
        t.androidResSource.set(t2.flatMap { it.output })
        t.androidResSourceRuntime.set(t2.flatMap { it.outputRuntime })
      }
      // Optional: only exists for Android libraries.
      explodeAssetSourceTask?.let { t2 -> t.androidAssetsSource.set(t2.flatMap { it.output }) }
      // Optional: only exists for Android projects.
      t.testInstrumentationRunner.set(testInstrumentationRunner)
      t.output.set(outputPaths.syntheticProjectPath)
    }
  }

  protected fun kaptConf(): Configuration? = try {
    project.configurations.getByName(kaptConfigurationName)
  } catch (_: UnknownDomainObjectException) {
    null
  }

  protected fun annotationProcessorConf(): Configuration? = try {
    project.configurations.getByName(annotationProcessorConfigurationName)
  } catch (_: UnknownDomainObjectException) {
    null
  }
}
