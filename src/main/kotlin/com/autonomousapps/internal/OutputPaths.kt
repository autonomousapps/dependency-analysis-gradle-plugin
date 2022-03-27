@file:Suppress("HasPlatformType")

package com.autonomousapps.internal

import org.gradle.api.Project

internal const val ROOT_DIR = "reports/dependency-analysis"

internal class OutputPaths(
  private val project: Project,
  variantName: String
) {

  private fun file(path: String) = project.layout.buildDirectory.file(path)
  private fun dir(path: String) = project.layout.buildDirectory.dir(path)

  private val variantDirectory = "$ROOT_DIR/$variantName"
  private val intermediatesDir = "${variantDirectory}/intermediates"

  val artifactsPath = file("${intermediatesDir}/artifacts.json")
  val artifactsPrettyPath = file("${intermediatesDir}/artifacts-pretty.json")
  val allDeclaredDepsPath = file("${intermediatesDir}/all-declared-dependencies.json")
  val allDeclaredDepsPrettyPath = file("${intermediatesDir}/all-declared-dependencies-pretty.json")
  val inlineUsagePath = file("${intermediatesDir}/inline-usage.json")
  val androidResPath = file("${intermediatesDir}/android-re.json")
  val androidResToResUsagePath = file("${intermediatesDir}/android-res-by-res-usage.json")
  val manifestPackagesPath = file("${intermediatesDir}/manifest-packages.json")
  val serviceLoaderDependenciesPath = file("${intermediatesDir}/service-loaders.json")
  val nativeDependenciesPath = file("${intermediatesDir}/native-libs.json")
  val androidLintersPath = file("${intermediatesDir}/android-linters.json")
  val declaredProcPath = file("${intermediatesDir}/procs-declared.json")
  val declaredProcPrettyPath = file("${intermediatesDir}/procs-declared-pretty.json")
  val abiAnalysisPath = file("${intermediatesDir}/abi.json")
  val abiDumpPath = file("${variantDirectory}/abi-dump.txt")
  val dependenciesDir = dir("${variantDirectory}/dependencies")
  val explodedSourcePath = file("${intermediatesDir}/exploded-source.json")
  val explodingBytecodePath = file("${intermediatesDir}/exploding-bytecode.json")
  val syntheticProjectPath = file("${intermediatesDir}/synthetic-project.json")
  val tracesPath = file("${variantDirectory}/traces.json")

  /*
   * Graph-related tasks
   */

  private val graphDir = "${variantDirectory}/graph"
  val compileGraphPath = file("${graphDir}/graph-compile.json")
  val compileGraphDotPath = file("${graphDir}/graph-compile.gv")
}

/**
 * Differs from [OutputPaths] in that this is for project-aggregator tasks that don't have variants.
 */
internal class NoVariantOutputPaths(private val project: Project) {

  @Suppress("SameParameterValue")
  private fun file(path: String) = project.layout.buildDirectory.file(path)

  val locationsPath = file("$ROOT_DIR/declarations.json")

  /*
   * Advice-related tasks.
   */

  val unfilteredAdvicePath = file("$ROOT_DIR/unfiltered-advice.json")
  val dependencyUsagesPath = file("$ROOT_DIR/usages-dependencies.json")
  val annotationProcessorUsagesPath = file("$ROOT_DIR/usages-annotation-processors.json")
  val filteredAdvicePath = file("$ROOT_DIR/final-advice.json")
  val consoleReportPath = file("$ROOT_DIR/project-health-report.txt")
}

/**
 * This is for the holistic, root-level aggregate reports.
 */
internal class RootOutputPaths(private val project: Project) {

  private fun file(path: String) = project.layout.buildDirectory.file(path)

  val buildHealthPath = file("$ROOT_DIR/build-health-report.json")
  val consoleReportPath = file("$ROOT_DIR/build-health-report.txt")
  val shouldFailPath = file("$ROOT_DIR/should-fail.txt")
}

internal class RedundantSubPluginOutputPaths(private val project: Project) {

  @Suppress("SameParameterValue")
  private fun file(path: String) = project.layout.buildDirectory.file(path)

  /**
   * This path doesn't use variants because the task that uses it only ever has one instance
   * registered.
   */
  val pluginJvmAdvicePath = file("$ROOT_DIR/advice-plugin-jvm.json")
}

// TODO used by tests
fun getVariantDirectory(variantName: String) = "$ROOT_DIR/$variantName"
fun getUsagesPath(variantName: String) = "${getVariantDirectory(variantName)}/traces.json"
fun getGraphPerVariantPath(variantName: String) = "${getVariantDirectory(variantName)}/graph/graph-compile.json"
fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"
fun getAdviceConsolePath(variantName: String) = "${getVariantDirectory(variantName)}/advice-console.txt"
fun getAdvicePathV2() = "$ROOT_DIR/final-advice.json"
fun getAggregateAdvicePath() = "$ROOT_DIR/advice-all-variants.json"
fun getAggregateAdvicePathV2() = "$ROOT_DIR/final-advice.json"
fun getStrictAdvicePath() = "$ROOT_DIR/advice-holistic-strict.json"
fun getMinimizedAdvicePath() = "$ROOT_DIR/advice-holistic-minimized.json"
fun getFinalAdvicePath() = "$ROOT_DIR/advice-holistic.json"
fun getFinalAdvicePathV2() = "$ROOT_DIR/build-health-report.json"
fun getRipplesPath() = "$ROOT_DIR/ripples.json"
