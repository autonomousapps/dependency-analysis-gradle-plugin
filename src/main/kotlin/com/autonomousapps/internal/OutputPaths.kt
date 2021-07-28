@file:Suppress("HasPlatformType")

package com.autonomousapps.internal

import org.gradle.api.Project

internal const val ROOT_DIR = "reports/dependency-analysis"

internal class OutputPaths(private val project: Project, variantName: String) {

  private fun layout(path: String) = project.layout.buildDirectory.file(path)

  private val variantDirectory = "$ROOT_DIR/$variantName"
  private val intermediatesDir = "${variantDirectory}/intermediates"

  val locationsPath = layout("${intermediatesDir}/locations.json")
  val artifactsPath = layout("${intermediatesDir}/artifacts.json")
  val artifactsPrettyPath = layout("${intermediatesDir}/artifacts-pretty.json")
  val variantFilesPath = layout("${intermediatesDir}/variant-files.json")
  val allUsedClassesPath = layout("${intermediatesDir}/all-used-classes.json")
  val allUsedClassesPrettyPath = layout("${intermediatesDir}/all-used-classes-pretty.json")
  val allDeclaredDepsPath = layout("${intermediatesDir}/all-declared-dependencies.json")
  val allDeclaredDepsPrettyPath =
    layout("${intermediatesDir}/all-declared-dependencies-pretty.json")
  val importsPath = layout("${intermediatesDir}/imports.json")
  val inlineUsagePath = layout("${intermediatesDir}/inline-usage.json")
  val constantUsagePath = layout("${intermediatesDir}/constant-usage.json")
  val androidResToSourceUsagePath = layout("${intermediatesDir}/android-res-by-source-usage.json")
  val androidResToResUsagePath = layout("${intermediatesDir}/android-res-by-res-usage.json")
  val generalUsagePath = layout("${intermediatesDir}/general-usage.json")
  val manifestPackagesPath = layout("${intermediatesDir}/manifest-packages.json")
  val allComponentsPath = layout("${intermediatesDir}/all-components-with-transitives.json")
  val unusedComponentsPath = layout("${intermediatesDir}/unused-components-with-transitives.json")
  val usedTransitiveDependenciesPath =
    layout("${intermediatesDir}/used-transitive-dependencies.json")
  val usedVariantDependenciesPath = layout("${intermediatesDir}/used-variant-dependencies.json")
  val serviceLoaderDependenciesPath = layout("${intermediatesDir}/service-loaders.json")
  val nativeDependenciesPath = layout("${intermediatesDir}/native-libs.json")
  val androidLintersPath = layout("${intermediatesDir}/android-linters.json")
  val declaredProcPath = layout("${intermediatesDir}/procs-declared.json")
  val declaredProcPrettyPath = layout("${intermediatesDir}/procs-declared-pretty.json")
  val unusedProcPath = layout("${intermediatesDir}/procs-unused.json")
  val abiAnalysisPath = layout("${intermediatesDir}/abi.json")
  val abiDumpPath = layout("${variantDirectory}/abi-dump.txt")
  val advicePath = layout("${variantDirectory}/advice.json")
  val advicePrettyPath = layout("${variantDirectory}/advice-pretty.json")
  val adviceConsolePath = layout("${variantDirectory}/advice-console.json")
  val adviceConsolePrettyPath = layout("${variantDirectory}/advice-console-pretty.json")
  val adviceConsoleTxtPath = layout("${variantDirectory}/advice-console.txt")

  /*
   * Graph-related tasks
   */

  private val graphDir = "${variantDirectory}/graph"
  val compileGraphPath = layout("${graphDir}/graph-compile.json")
  val testCompileGraphPath = layout("${graphDir}/graph-test-compile.json")
  val compileGraphDotPath = layout("${graphDir}/graph-compile.gv")
  val testCompileGraphDotPath = layout("${graphDir}/graph-test-compile.gv")
  val reasonableDependenciesPath = layout("${intermediatesDir}/reasonable-dependencies.json")
  val graphReasonPath = layout("${graphDir}/graph-reason.gv")

  /*
   * Redundant plugin tasks
   */

  val pluginKaptAdvicePath = layout("${getVariantDirectory(variantName)}/advice-plugin-kapt.json")
}

/**
 * Differs from [OutputPaths] in that this is for project-aggregator tasks that don't have variants.
 */
internal class NoVariantOutputPaths(private val project: Project) {

  /*
   * Advice-related tasks.
   */

  val aggregateAdvicePath = layout("$ROOT_DIR/advice-all-variants.json")
  val aggregateAdvicePrettyPath = layout("$ROOT_DIR/advice-all-variants-pretty.json")

  /*
   * Graph-related tasks.
   */

  val aggregateGraphJsonPath = layout("$ROOT_DIR/graph-all-variants.json")
  val aggregateGraphDotPath = layout("$ROOT_DIR/graph-all-variants.gv")
  val graphReasonPath = layout("$ROOT_DIR/graph-reason.gv")

  /*
   * Metrics-related tasks.
   */

  val projMetricsPath = layout("$ROOT_DIR/proj-metrics.json")
  val projGraphDotPath = layout("$ROOT_DIR/proj-graph.gv")
  val projGraphModDotPath = layout("$ROOT_DIR/proj-mod-graph.gv")

  @Suppress("SameParameterValue")
  private fun layout(path: String) = project.layout.buildDirectory.file(path)
}

/**
 * This is for the holistic, root-level aggregate reports.
 */
internal class RootOutputPaths(private val project: Project) {

  private fun layout(path: String) = project.layout.buildDirectory.file(path)

  val strictAdvicePath = layout("$ROOT_DIR/advice-holistic-strict.json")
  val strictAdvicePrettyPath = layout("$ROOT_DIR/advice-holistic-strict-pretty.json")

  val minimizedAdvicePath = layout("$ROOT_DIR/advice-holistic-minimized.json")
  val minimizedAdvicePrettyPath = layout("$ROOT_DIR/advice-holistic-minimized-pretty.json")

  val finalAdvicePath = layout("$ROOT_DIR/advice-holistic.json")

  /* Graph paths. */

  val mergedGraphJsonPath = layout("$ROOT_DIR/merged-graph.json")
  val mergedGraphDotPath = layout("$ROOT_DIR/merged-graph.gv")
  val mergedGraphRevJsonPath = layout("$ROOT_DIR/merged-graph-rev.json")
  val mergedGraphRevDotPath = layout("$ROOT_DIR/merged-graph-rev.gv")
  val mergedGraphRevSubDotPath = layout("$ROOT_DIR/merged-graph-rev-sub.gv")

  val buildMetricsPath = layout("$ROOT_DIR/metrics.json")
  val ripplesPath = layout("$ROOT_DIR/ripples.json")
}

internal class RedundantSubPluginOutputPaths(private val project: Project) {

  @Suppress("SameParameterValue")
  private fun layout(path: String) = project.layout.buildDirectory.file(path)

  /**
   * This path doesn't use variants because the task that uses it only ever has one instance
   * registered.
   */
  val pluginJvmAdvicePath = layout("$ROOT_DIR/advice-plugin-jvm.json")
}

// TODO used by tests
fun getVariantDirectory(variantName: String) = "$ROOT_DIR/$variantName"
fun getAllUsedClassesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/all-used-classes.json"

fun getUnusedDirectDependenciesPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/unused-components-with-transitives.json"

fun getAbiAnalysisPath(variantName: String) =
  "${getVariantDirectory(variantName)}/intermediates/abi.json"

fun getGraphPerVariantPath(variantName: String) =
  "${getVariantDirectory(variantName)}/graph/graph-compile.json"

fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"
fun getAdviceConsolePath(variantName: String) =
  "${getVariantDirectory(variantName)}/advice-console.txt"

fun getAggregateAdvicePath() = "$ROOT_DIR/advice-all-variants.json"

fun getStrictAdvicePath() = "$ROOT_DIR/advice-holistic-strict.json"
fun getMinimizedAdvicePath() = "$ROOT_DIR/advice-holistic-minimized.json"
fun getFinalAdvicePath() = "$ROOT_DIR/advice-holistic.json"

fun getRipplesPath() = "$ROOT_DIR/ripples.json"

