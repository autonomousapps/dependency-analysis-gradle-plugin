// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.AbstractExtension
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.tasks.*
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

  /** Computes how this project really uses its dependencies, without consideration for user reporting preferences. */
  fun registerComputeUsagesTask(
    checkSuperClasses: Provider<Boolean>,
    checkBinaryCompat: Provider<Boolean>,
    isKaptApplied: Provider<Boolean>,
    graphViewTask: TaskProvider<GraphViewTask>,
    findDeclarationsTask: TaskProvider<FindDeclarationsTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    duplicateClassesCompile: TaskProvider<DiscoverClasspathDuplicationTask>,
    duplicateClassesRuntime: TaskProvider<DiscoverClasspathDuplicationTask>,
  ): TaskProvider<ComputeUsagesTask>

  /** Produces a report of the types, and their kinds (local, other project, external module) used by this project. */
  fun registerComputeTypeUsageTask(
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
    explodeJarTask: TaskProvider<ExplodeJarTask>,
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    dagpExtension: AbstractExtension,
  ): TaskProvider<ComputeTypeUsageTask>

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
