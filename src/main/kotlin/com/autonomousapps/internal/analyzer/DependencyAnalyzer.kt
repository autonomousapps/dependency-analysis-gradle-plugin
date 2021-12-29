@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.*
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Abstraction for differentiating between android-app, android-lib, and java-lib projects.
 */
internal interface DependencyAnalyzer {
  /** E.g., `flavorDebug` */
  val variantName: String

  /** E.g., 'flavor' */
  val flavorName: String?

  /** E.g., 'debug' */
  val buildType: String?

  /** E.g., `FlavorDebug` */
  val variantNameCapitalized: String

  /** E.g., "compileClasspath", "debugCompileClasspath". */
  val compileConfigurationName: String

  /** E.g., "testCompileClasspath", "debugTestCompileClasspath". */
  @Deprecated("v1 legacy. Replace with compileConfigurationName")
  val testCompileConfigurationName: String

  /** E.g., "kaptDebug" */
  val kaptConfigurationName: String

  /** E.g., "annotationProcessorDebug" */
  val annotationProcessorConfigurationName: String

  val attributeValueJar: String

  val kotlinSourceFiles: FileTree
  val javaSourceFiles: FileTree?
  val javaAndKotlinSourceFiles: FileTree?

  val isDataBindingEnabled: Boolean
  val isViewBindingEnabled: Boolean

  val testJavaCompileName: String
  val testKotlinCompileName: String

  fun registerCreateVariantFilesTask(): TaskProvider<out CreateVariantFiles>

  /** This produces a report that lists all of the used classes (FQCN) in the project. */
  fun registerClassAnalysisTask(
    createVariantFiles: TaskProvider<out CreateVariantFiles>
  ): TaskProvider<out ClassAnalysisTask>

  fun registerByteCodeSourceExploderTask(): TaskProvider<out ByteCodeSourceExploderTask>

  fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask>? = null

  fun registerManifestComponentsExtractionTask(): TaskProvider<ManifestComponentsExtractionTask>? = null

  fun registerAndroidResToSourceAnalysisTask(
    manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>
  ): TaskProvider<AndroidResToSourceAnalysisTask>? = null

  fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask>? = null

  fun registerFindAndroidResTask(): TaskProvider<FindAndroidResTask>? = null
  fun registerExplodeXmlSourceTask(): TaskProvider<XmlSourceExploderTask>? = null

  fun registerFindNativeLibsTask(
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindNativeLibsTask>? = null

  fun registerFindNativeLibsTask2(): TaskProvider<FindNativeLibsTask2>? = null

  fun registerFindAndroidLintersTask(
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindAndroidLinters>? = null

  fun registerFindAndroidLintersTask2(): TaskProvider<FindAndroidLinters2>? = null

  fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>,
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindDeclaredProcsTask>

  fun registerFindDeclaredProcsTask(
    inMemoryCache: Provider<InMemoryCache>
  ): TaskProvider<FindDeclaredProcsTask2>

  fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>,
    importFinder: TaskProvider<ImportFinderTask>
  ): TaskProvider<FindUnusedProcsTask>

  /**
   * This is a no-op for `com.android.application` and JVM `application` projects (including Spring Boot), since they
   * have no meaningful ABI.
   */
  fun registerAbiAnalysisTask(
    analyzeJarTask: TaskProvider<AnalyzeJarTask>,
    abiExclusions: Provider<String>
  ): TaskProvider<AbiAnalysisTask>? = null

  /**
   * This is a no-op for `com.android.application` and JVM `application` projects (including Spring Boot), since they
   * have no meaningful ABI.
   */
  fun registerAbiAnalysisTask2(abiExclusions: Provider<String>): TaskProvider<AbiAnalysisTask2>? = null
}

internal abstract class AbstractDependencyAnalyzer(
  protected val project: Project
) : DependencyAnalyzer {

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
}
