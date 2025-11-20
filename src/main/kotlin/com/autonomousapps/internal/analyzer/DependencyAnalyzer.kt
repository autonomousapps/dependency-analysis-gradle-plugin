// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.extension.ExclusionsHandler
import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.internal.artifactsFor
import com.autonomousapps.internal.utils.project.buildPath
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import kotlin.text.set

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

  /** Lists the dependencies declared for building the project, along with their physical artifacts (jars). */
  fun registerArtifactsReportTask(): TaskProvider<ArtifactsReportTask>

  fun registerByteCodeSourceExploderTask(): TaskProvider<ClassListExploderTask>
  fun registerCodeSourceExploderTask(): TaskProvider<out CodeSourceExploderTask>
  fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask>? = null
  fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask>? = null
  fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask>? = null
  fun registerExplodeAssetSourceTask(): TaskProvider<AssetSourceExploderTask>? = null
  fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask>
  fun registerFindAndroidLintersTask(): TaskProvider<FindAndroidLinters>? = null
  fun registerFindAndroidAssetProvidersTask(): TaskProvider<FindAndroidAssetProviders>? = null
  fun registerFindDeclaredProcsTask(): TaskProvider<FindDeclaredProcsTask>
  fun registerGraphViewTask(findDeclarationsTask: TaskProvider<FindDeclarationsTask>): TaskProvider<GraphViewTask>

  /**
   * This is a no-op for `com.android.application` and JVM `application` projects (including Spring Boot), since they
   * have no meaningful ABI.
   */
  fun registerAbiAnalysisTask(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask>? = null

  fun registerAndroidScoreTask(
    synthesizeDependenciesTask: TaskProvider<SynthesizeDependenciesTask>,
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>,
  ): TaskProvider<AndroidScoreTask>? = null
}

internal abstract class AbstractDependencyAnalyzer(
  protected val project: Project,
) : DependencyAnalyzer {

  // Always null for JVM projects. May be null for Android projects.
  override val testInstrumentationRunner: Provider<String> = project.provider { null }

  final override fun registerArtifactsReportTask(): TaskProvider<ArtifactsReportTask> {
    return project.tasks.register("artifactsReport$taskNameSuffix", ArtifactsReportTask::class.java) {
      it.setConfiguration(project.configurations.named(compileConfigurationName)) { c ->
        c.artifactsFor(attributeValueJar)
      }
      it.buildPath.set(project.buildPath(compileConfigurationName))

      it.output.set(outputPaths.compileArtifactsPath)
      it.excludedIdentifiersOutput.set(outputPaths.excludedIdentifiersPath)
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
