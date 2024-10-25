// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("HasPlatformType")

package com.autonomousapps.internal

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal const val ROOT_DIR = "reports/dependency-analysis"

internal class OutputPaths(
  private val project: Project,
  variantName: String
) {

  private fun file(path: String): Provider<RegularFile> = project.layout.buildDirectory.file(path)
  private fun dir(path: String): Provider<Directory> = project.layout.buildDirectory.dir(path)

  private val variantDirectory = "$ROOT_DIR/$variantName"
  private val intermediatesDir = "${variantDirectory}/intermediates"

  val compileArtifactsPath = file("${intermediatesDir}/artifacts.json")
  val runtimeArtifactsPath = file("${intermediatesDir}/artifacts-runtime.json")
  val externalDependenciesPath = file("${intermediatesDir}/external-dependencies.txt")
  val duplicateCompileClasspathPath = file("${intermediatesDir}/duplicate-classes-compile.json")
  val duplicateCompileRuntimePath = file("${intermediatesDir}/duplicate-classes-runtime.json")
  val allDeclaredDepsPath = file("${intermediatesDir}/exploded-jars.json")
  val inlineUsagePath = file("${intermediatesDir}/inline-usage.json")
  val typealiasUsagePath = file("${intermediatesDir}/typealias-usage.json")
  val inlineUsageErrorsPath = file("${intermediatesDir}/inline-usage-errors.txt")
  val androidResPath = file("${intermediatesDir}/android-res.json")
  val androidResToResUsagePath = file("${intermediatesDir}/android-res-by-res-usage.json")
  val androidAssetSourcePath = file("${intermediatesDir}/exploded-assets.json")
  val manifestPackagesPath = file("${intermediatesDir}/manifest-packages.json")
  val serviceLoaderDependenciesPath = file("${intermediatesDir}/service-loaders.json")
  val nativeDependenciesPath = file("${intermediatesDir}/native-libs.json")
  val androidLintersPath = file("${intermediatesDir}/android-linters.json")
  val androidAssetsPath = file("${intermediatesDir}/android-asset-providers.json")
  val declaredProcPath = file("${intermediatesDir}/procs-declared.json")
  val abiAnalysisPath = file("${intermediatesDir}/abi.json")
  val abiDumpPath = file("${variantDirectory}/abi-dump.txt")
  val dependenciesDir = dir("${variantDirectory}/dependencies")
  val explodedSourcePath = file("${intermediatesDir}/exploded-source.json")
  val explodingBytecodePath = file("${intermediatesDir}/exploding-bytecode.json")
  val syntheticProjectPath = file("${intermediatesDir}/synthetic-project.json")
  val dependencyTraceReportPath = file("${variantDirectory}/dependency-trace-report.json")
  val androidScorePath = file("${variantDirectory}/android-score.json")

  /*
   * Graph-related tasks
   */

  private val graphDir = "${variantDirectory}/graph"
  val compileGraphPath = file("${graphDir}/graph-compile.json")
  val compileGraphDotPath = file("${graphDir}/graph-compile.gv")
  val compileNodesPath = file("${graphDir}/graph-compile-nodes.json")
  val runtimeGraphPath = file("${graphDir}/graph-runtime.json")
  val runtimeGraphDotPath = file("${graphDir}/graph-runtime.gv")
  val compileDominatorConsolePath = file("${graphDir}/graph-dominator.txt")
  val runtimeDominatorConsolePath = file("${graphDir}/graph-dominator-runtime.txt")
  val compileDominatorGraphPath = file("${graphDir}/graph-dominator.gv")
  val runtimeDominatorGraphPath = file("${graphDir}/graph-dominator-runtime.gv")
  val compileDominatorJsonPath = file("${graphDir}/graph-dominator.json")
  val runtimeDominatorJsonPath = file("${graphDir}/graph-dominator-runtime.json")

  val projectGraphDir = dir("$graphDir/project")
}

/**
 * Differs from [OutputPaths] in that this is for project-aggregator tasks that don't have variants.
 */
@Suppress("SameParameterValue")
internal class NoVariantOutputPaths(private val project: Project) {

  private fun file(path: String): Provider<RegularFile> = project.layout.buildDirectory.file(path)
  private fun dir(path: String): Provider<Directory> = project.layout.buildDirectory.dir(path)

  val locationsPath = file("$ROOT_DIR/declarations.json")
  val resolvedDepsPath = file("$ROOT_DIR/resolved-dependencies-report.txt")

  /*
   * Advice-related tasks.
   */

  val unfilteredAdvicePath = file("$ROOT_DIR/unfiltered-advice.json")
  val bundledTracesPath = file("$ROOT_DIR/bundled-traces.json")
  val dependencyUsagesPath = file("$ROOT_DIR/usages-dependencies.json")
  val annotationProcessorUsagesPath = file("$ROOT_DIR/usages-annotation-processors.json")
  val filteredAdvicePath = file("$ROOT_DIR/final-advice.json")
  val consoleReportPath = file("$ROOT_DIR/project-health-report.txt")
}

/**
 * This is for the holistic, root-level aggregate reports.
 */
internal class RootOutputPaths(private val project: Project) {

  private fun file(path: String): Provider<RegularFile> = project.layout.buildDirectory.file(path)

  val duplicateDependenciesPath = file("$ROOT_DIR/duplicate-dependencies-report.json")
  val buildHealthPath = file("$ROOT_DIR/build-health-report.json")
  val consoleReportPath = file("$ROOT_DIR/build-health-report.txt")
  val shouldFailPath = file("$ROOT_DIR/should-fail.txt")
}

internal class RedundantSubPluginOutputPaths(private val project: Project) {

  @Suppress("SameParameterValue")
  private fun file(path: String): Provider<RegularFile> = project.layout.buildDirectory.file(path)

  /**
   * This path doesn't use variants because the task that uses it only ever has one instance
   * registered.
   */
  val pluginJvmAdvicePath = file("$ROOT_DIR/advice-plugin-jvm.json")
}

// TODO used by tests
fun getAdvicePathV2() = "$ROOT_DIR/final-advice.json"
fun getAggregateAdvicePathV2() = "$ROOT_DIR/final-advice.json"
fun getFinalAdvicePathV2() = "$ROOT_DIR/build-health-report.json"
fun getDuplicateDependenciesReport() = "$ROOT_DIR/duplicate-dependencies-report.json"
fun getResolvedDependenciesReport() = "$ROOT_DIR/resolved-dependencies-report.txt"
