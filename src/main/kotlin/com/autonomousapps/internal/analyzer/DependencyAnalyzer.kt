// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.model.source.SourceKind
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.get

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

  protected fun kaptConf(): Configuration? = try {
    project.configurations[kaptConfigurationName]
  } catch (_: UnknownDomainObjectException) {
    null
  }

  protected fun annotationProcessorConf(): Configuration? = try {
    project.configurations[annotationProcessorConfigurationName]
  } catch (_: UnknownDomainObjectException) {
    null
  }
}
