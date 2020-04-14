@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.analyzer

import com.autonomousapps.services.InMemoryCache
import com.autonomousapps.tasks.AbiAnalysisTask
import com.autonomousapps.tasks.AndroidResToResToResAnalysisTask
import com.autonomousapps.tasks.AndroidResToSourceAnalysisTask
import com.autonomousapps.tasks.ClassAnalysisTask
import com.autonomousapps.tasks.DependencyReportTask
import com.autonomousapps.tasks.FindDeclaredProcsTask
import com.autonomousapps.tasks.FindUnusedProcsTask
import com.autonomousapps.tasks.LocateDependenciesTask
import com.autonomousapps.tasks.ManifestPackageExtractionTask
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Abstraction for differentiating between android-app, android-lib, and java-lib projects.
 */
internal interface DependencyAnalyzer<T : ClassAnalysisTask> {
  /**
   * E.g., `flavorDebug`
   */
  val variantName: String

  /**
   * E.g., `FlavorDebug`
   */
  val variantNameCapitalized: String

  val compileConfigurationName: String
  val runtimeConfigurationName: String

  val attribute: Attribute<String>
  val attributeValue: String
  val attributeValueRes: String?

  val kotlinSourceFiles: FileTree
  val javaSourceFiles: FileTree?
  val javaAndKotlinSourceFiles: FileTree?

  val isDataBindingEnabled: Boolean
  val isViewBindingEnabled: Boolean

  /**
   * This produces a report that lists all of the used classes (FQCN) in the project.
   */
  fun registerClassAnalysisTask(): TaskProvider<out T>

  fun registerManifestPackageExtractionTask(): TaskProvider<ManifestPackageExtractionTask>? = null

  fun registerAndroidResToSourceAnalysisTask(
    manifestPackageExtractionTask: TaskProvider<ManifestPackageExtractionTask>
  ): TaskProvider<AndroidResToSourceAnalysisTask>? = null

  fun registerAndroidResToResAnalysisTask(): TaskProvider<AndroidResToResToResAnalysisTask>? = null

  fun registerFindDeclaredProcsTask(
    inMemoryCacheProvider: Provider<InMemoryCache>,
    locateDependenciesTask: TaskProvider<LocateDependenciesTask>
  ): TaskProvider<FindDeclaredProcsTask>

  fun registerFindUnusedProcsTask(
    findDeclaredProcs: TaskProvider<FindDeclaredProcsTask>
  ): TaskProvider<FindUnusedProcsTask>

  /**
   * This is a no-op for com.android.application projects, since they have no meaningful ABI.
   */
  fun registerAbiAnalysisTask(
    dependencyReportTask: TaskProvider<DependencyReportTask>
  ): TaskProvider<AbiAnalysisTask>? = null
}

// Best guess as to path to kapt-generated Java stubs
internal fun getKaptStubs(project: Project, variantName: String): FileTree =
  project.layout.buildDirectory.asFileTree.matching {
    include("**/kapt*/**/${variantName}/**/*.java")
  }
