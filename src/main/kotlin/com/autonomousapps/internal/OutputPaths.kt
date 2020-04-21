package com.autonomousapps.internal

import org.gradle.api.Project

internal const val ROOT_DIR = "reports/dependency-analysis"

// TODO move these classes to a non-internal package and document use
class OutputPaths(private val project: Project, variantName: String) {

  private fun layout(path: String) = project.layout.buildDirectory.file(path)

  private val variantDirectory = "$ROOT_DIR/$variantName"
  private val intermediatesDir = "${variantDirectory}/intermediates"

  val locationsPath = layout("${intermediatesDir}/locations.json")
  val artifactsPath = layout("${intermediatesDir}/artifacts.json")
  val artifactsPrettyPath = layout("${intermediatesDir}/artifacts-pretty.json")
  val allUsedClassesPath = layout("${intermediatesDir}/all-used-classes.txt")
  val allDeclaredDepsPath = layout("${intermediatesDir}/all-declared-dependencies.json")
  val allDeclaredDepsPrettyPath = layout("${intermediatesDir}/all-declared-dependencies-pretty.json")
  val importsPath = layout("${intermediatesDir}/imports.json")
  val inlineMembersPath = layout("${intermediatesDir}/inline-members.json")
  val inlineUsagePath = layout("${intermediatesDir}/inline-usage.json")
  val constantUsagePath = layout("${intermediatesDir}/constant-usage.json")
  val androidResToSourceUsagePath = layout("${intermediatesDir}/android-res-by-source-usage.json")
  val androidResToResUsagePath = layout("${intermediatesDir}/android-res-by-res-usage.json")
  val manifestPackagesPath = layout("${intermediatesDir}/manifest-packages.json")
  val unusedDirectDependenciesPath = layout("${intermediatesDir}/unused-direct-dependencies.json")
  val usedTransitiveDependenciesPath = layout("${intermediatesDir}/used-transitive-dependencies.json")
  val serviceLoaderDependenciesPath = layout("${intermediatesDir}/service-loaders.json")
  val declaredProcPath = layout("${intermediatesDir}/procs-declared.json")
  val declaredProcPrettyPath = layout("${intermediatesDir}/procs-declared-pretty.json")
  val unusedProcPath = layout("${intermediatesDir}/procs-unused.json")
  val abiAnalysisPath = layout("${intermediatesDir}/abi.json")
  val abiDumpPath = layout("${intermediatesDir}/abi-dump.txt")
  val advicePath = layout("${variantDirectory}/advice.json")
  val advicePrettyPath = layout("${variantDirectory}/advice-pretty.json")
  val adviceConsolePath = layout("${variantDirectory}/advice-console.json")
  val adviceConsolePrettyPath = layout("${variantDirectory}/advice-console-pretty.json")
  val adviceConsoleTxtPath = layout("${variantDirectory}/advice-console.txt")

  /*
   * Redundant plugin tasks
   */

  val pluginKaptAdvicePath = layout("${getVariantDirectory(variantName)}/advice-plugin-kapt.json")
}

class RootOutputPaths(private val project: Project) {

  private fun layout(path: String) = project.layout.buildDirectory.file(path)

  val misusedDependenciesAggregatePath = layout("${ROOT_DIR}/intermediates/misused-dependencies.json")
  val misusedDependenciesAggregatePrettyPath = layout("${ROOT_DIR}/intermediates/misused-dependencies-pretty.json")
  val abiAggregatePath = layout("${ROOT_DIR}/intermediates/abi.json")
  val abiAggregatePrettyPath = layout("${ROOT_DIR}/intermediates/abi-pretty.json")
  val adviceAggregatePath = layout("${ROOT_DIR}/advice.json")
  val adviceAggregatePrettyPath = layout("${ROOT_DIR}/advice-pretty.json")
}

class RedundantSubPluginOutputPaths(
  private val project: Project
) {

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
fun getAllUsedClassesPath(variantName: String) = "${getVariantDirectory(variantName)}/intermediates/all-used-classes.txt"
fun getUnusedDirectDependenciesPath(variantName: String) = "${getVariantDirectory(variantName)}/intermediates/unused-direct-dependencies.json"
fun getAbiAnalysisPath(variantName: String) = "${getVariantDirectory(variantName)}/intermediates/abi.json"
fun getAdvicePath(variantName: String) = "${getVariantDirectory(variantName)}/advice.json"
fun getAdviceAggregatePath() = "$ROOT_DIR/advice.json"
