@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.internal.OutputPaths
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.UnknownTaskException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Abstraction for differentiating between android-app, android-lib, and java-lib projects.  */
internal interface DependencyAnalyzer {
  /** E.g., `flavorDebug` */
  val variantName: String

  /** E.g., 'flavor' */
  val flavorName: String?

  /** E.g., 'debug' */
  val buildType: String?

  val kind: SourceSetKind

  /** E.g., `FlavorDebug` */
  val variantNameCapitalized: String

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
  val testInstrumentationRunner: String?

  val attributeValueJar: String

  val kotlinSourceFiles: FileCollection
  val javaSourceFiles: FileCollection?
  val groovySourceFiles: FileCollection
  val scalaSourceFiles: FileCollection

  val isDataBindingEnabled: Boolean
  val isViewBindingEnabled: Boolean

  val testJavaCompileName: String
  val testKotlinCompileName: String

  val outputPaths: OutputPaths

  fun registerByteCodeSourceExploderTask(): TaskProvider<out ByteCodeSourceExploderTask>

  fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask>? = null

  fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask>? = null
  fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask>? = null
  fun registerExplodeAssetSourceTask(): TaskProvider<AssetSourceExploderTask>? = null

  fun registerFindNativeLibsTask(): TaskProvider<FindNativeLibsTask>? = null

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
    synthesizeProjectViewTask: TaskProvider<SynthesizeProjectViewTask>
  ): TaskProvider<AndroidScoreTask>? = null
}

internal abstract class AbstractDependencyAnalyzer(
  protected val project: Project
) : DependencyAnalyzer {

  // Always null for JVM projects. May be null for Android projects.
  override val testInstrumentationRunner: String? = null

  protected val testJavaCompile by lazy {
    try {
      project.tasks.named<JavaCompile>(testJavaCompileName)
    } catch (e: UnknownTaskException) {
      null
    }
  }

  protected val testKotlinCompile by lazy {
    try {
      project.tasks.named<KotlinCompile>(testKotlinCompileName)
    } catch (e: UnknownTaskException) {
      null
    } catch (e: NoClassDefFoundError) {
      null
    }
  }

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
