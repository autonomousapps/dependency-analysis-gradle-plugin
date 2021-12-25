@file:Suppress("HasPlatformType")

package com.autonomousapps.internal

import org.gradle.api.Project

internal const val ROOT_DIR = "reports/dependency-analysis"

internal class OutputPaths(private val project: Project, variantName: String) {

  private fun file(path: String) = project.layout.buildDirectory.file(path)
  private fun dir(path: String) = project.layout.buildDirectory.dir(path)

  private val variantDirectory = "$ROOT_DIR/$variantName"
  private val intermediatesDir = "${variantDirectory}/intermediates"

  val locationsPath = file("${intermediatesDir}/locations.json")
  val artifactsPath = file("${intermediatesDir}/artifacts.json")
  val artifactsPrettyPath = file("${intermediatesDir}/artifacts-pretty.json")
  val variantFilesPath = file("${intermediatesDir}/variant-files.json")
  val allUsedClassesPath = file("${intermediatesDir}/all-used-classes.json")
  val allUsedClassesPrettyPath = file("${intermediatesDir}/all-used-classes-pretty.json")
  val allDeclaredDepsPath = file("${intermediatesDir}/all-declared-dependencies.json")
  val allDeclaredDepsPrettyPath = file("${intermediatesDir}/all-declared-dependencies-pretty.json")
  val importsPath = file("${intermediatesDir}/imports.json")
  val inlineUsagePath = file("${intermediatesDir}/inline-usage.json")
  val constantUsagePath = file("${intermediatesDir}/constant-usage.json")
  val androidResPath = file("${intermediatesDir}/android-re.json")
  val androidResToSourceUsagePath = file("${intermediatesDir}/android-res-by-source-usage.json")
  val androidResToResUsagePath = file("${intermediatesDir}/android-res-by-res-usage.json")
  val generalUsagePath = file("${intermediatesDir}/general-usage.json")
  val manifestPackagesPath = file("${intermediatesDir}/manifest-packages.json")
  val allComponentsPath = file("${intermediatesDir}/all-components-with-transitives.json")
  val unusedComponentsPath = file("${intermediatesDir}/unused-components-with-transitives.json")
  val usedTransitiveDependenciesPath = file("${intermediatesDir}/used-transitive-dependencies.json")
  val usedVariantDependenciesPath = file("${intermediatesDir}/used-variant-dependencies.json")
  val serviceLoaderDependenciesPath = file("${intermediatesDir}/service-loaders.json")
  val nativeDependenciesPath = file("${intermediatesDir}/native-libs.json")
  val androidLintersPath = file("${intermediatesDir}/android-linters.json")
  val declaredProcPath = file("${intermediatesDir}/procs-declared.json")
  val declaredProcPrettyPath = file("${intermediatesDir}/procs-declared-pretty.json")
  val unusedProcPath = file("${intermediatesDir}/procs-unused.json")
  val abiAnalysisPath = file("${intermediatesDir}/abi.json")
  val abiDumpPath = file("${variantDirectory}/abi-dump.txt")
  val advicePath = file("${variantDirectory}/advice.json")
  val advicePrettyPath = file("${variantDirectory}/advice-pretty.json")
  val adviceConsolePath = file("${variantDirectory}/advice-console.json")
  val adviceConsolePrettyPath = file("${variantDirectory}/advice-console-pretty.json")
  val adviceConsoleTxtPath = file("${variantDirectory}/advice-console.txt")
  val dependenciesDir = dir("${variantDirectory}/dependencies")
  val explodedSourcePath = file("${intermediatesDir}/exploded-source.json")
  val explodingBytecodePath = file("${intermediatesDir}/exploding-bytecode.json")
  val syntheticProject = file("${intermediatesDir}/synthetic-project.json")
  /** Separate from advice.json because I might map to that temporarily */
  val computedAdvicePath = file("${variantDirectory}/computed-advice.json")

  /*
   * Graph-related tasks
   */

  private val graphDir = "${variantDirectory}/graph"
  val compileGraphPath = file("${graphDir}/graph-compile.json")
  val testCompileGraphPath = file("${graphDir}/graph-test-compile.json")
  val compileGraphDotPath = file("${graphDir}/graph-compile.gv")
  val testCompileGraphDotPath = file("${graphDir}/graph-test-compile.gv")
  val reasonableDependenciesPath = file("${intermediatesDir}/reasonable-dependencies.json")
  val graphReasonPath = file("${graphDir}/graph-reason.gv")

  /*
   * Redundant plugin tasks
   */

  val pluginKaptAdvicePath = file("${getVariantDirectory(variantName)}/advice-plugin-kapt.json")
}

/**
 * Differs from [OutputPaths] in that this is for project-aggregator tasks that don't have variants.
 */
internal class NoVariantOutputPaths(private val project: Project) {

  val locationsPath = file("$ROOT_DIR/locations.json")

  /*
   * Advice-related tasks.
   */

  // v2
  val unfilteredAdvicePath = file("$ROOT_DIR/unfiltered-advice.json")
  val filteredAdvicePath = file("$ROOT_DIR/final-advice.json")
  val consoleReportPath = file("$ROOT_DIR/project-health-report.txt")

  // v1
  val aggregateAdvicePath = file("$ROOT_DIR/advice-all-variants.json")
  val aggregateAdvicePrettyPath = file("$ROOT_DIR/advice-all-variants-pretty.json")

  /*
   * Graph-related tasks.
   */

  val aggregateGraphJsonPath = file("$ROOT_DIR/graph-all-variants.json")
  val aggregateGraphDotPath = file("$ROOT_DIR/graph-all-variants.gv")
  val graphReasonPath = file("$ROOT_DIR/graph-reason.gv")

  /*
   * Metrics-related tasks.
   */

  val projMetricsPath = file("$ROOT_DIR/proj-metrics.json")
  val projGraphDotPath = file("$ROOT_DIR/proj-graph.gv")
  val projGraphModDotPath = file("$ROOT_DIR/proj-mod-graph.gv")

  @Suppress("SameParameterValue")
  private fun file(path: String) = project.layout.buildDirectory.file(path)
}

/**
 * This is for the holistic, root-level aggregate reports.
 */
internal class RootOutputPaths(private val project: Project) {

  private fun file(path: String) = project.layout.buildDirectory.file(path)

  // v2
  val buildHealthPath = file("$ROOT_DIR/build-health-report.json")
  val consoleReportPath = file("$ROOT_DIR/build-health-report.txt")

  // v1
  val strictAdvicePath = file("$ROOT_DIR/advice-holistic-strict.json")
  val strictAdvicePrettyPath = file("$ROOT_DIR/advice-holistic-strict-pretty.json")

  val minimizedAdvicePath = file("$ROOT_DIR/advice-holistic-minimized.json")
  val minimizedAdvicePrettyPath = file("$ROOT_DIR/advice-holistic-minimized-pretty.json")

  val finalAdvicePath = file("$ROOT_DIR/advice-holistic.json")

  /* Graph paths. */

  val mergedGraphJsonPath = file("$ROOT_DIR/merged-graph.json")
  val mergedGraphDotPath = file("$ROOT_DIR/merged-graph.gv")
  val mergedGraphRevJsonPath = file("$ROOT_DIR/merged-graph-rev.json")
  val mergedGraphRevDotPath = file("$ROOT_DIR/merged-graph-rev.gv")
  val mergedGraphRevSubDotPath = file("$ROOT_DIR/merged-graph-rev-sub.gv")

  val buildMetricsPath = file("$ROOT_DIR/metrics.json")
  val ripplesPath = file("$ROOT_DIR/ripples.json")
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
fun getAllUsedClassesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/all-used-classes.json"

fun getUnusedDirectDependenciesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/unused-components-with-transitives.json"

fun getAbiAnalysisPath(variantName: String) = "${getVariantDirectory(variantName)}/intermediates/abi.json"

fun getGraphPerVariantPath(variantName: String) = "${getVariantDirectory(variantName)}/graph/graph-compile.json"

fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"
fun getAdviceConsolePath(variantName: String) = "${getVariantDirectory(variantName)}/advice-console.txt"

fun getAggregateAdvicePath() = "$ROOT_DIR/advice-all-variants.json"
fun getAggregateAdvicePathV2() = "$ROOT_DIR/final-advice.json"

fun getStrictAdvicePath() = "$ROOT_DIR/advice-holistic-strict.json"
fun getMinimizedAdvicePath() = "$ROOT_DIR/advice-holistic-minimized.json"
fun getFinalAdvicePath() = "$ROOT_DIR/advice-holistic.json"
fun getFinalAdvicePathV2() = "$ROOT_DIR/build-health-report.json"

fun getRipplesPath() = "$ROOT_DIR/ripples.json"
