@file:Suppress("UnstableApiUsage", "SameParameterValue")

package com.autonomousapps

import org.gradle.api.GradleException
import org.gradle.api.Project

@Suppress("DEPRECATION") // forUseAtConfigurationTime()
object Flags {

  internal const val FLAG_CLEAR_ARTIFACTS = "dependency.analysis.clear.artifacts"
  internal const val FLAG_SILENT_WARNINGS = "dependency.analysis.warnings.silent"

  private const val FLAG_AUTO_APPLY = "dependency.analysis.autoapply"
  private const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
  private const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"
  private const val FLAG_PRINT_BUILD_HEALTH = "dependency.analysis.print.build.health"
  private const val FLAG_VARIANTS_ANALYSIS = "dependency.analysis.android.variants"

  internal fun Project.shouldAnalyzeTests() = getGradleOrSysProp(FLAG_TEST_ANALYSIS, true)
  internal fun Project.shouldAutoApply() = getGradleOrSysProp(FLAG_AUTO_APPLY, true)
  internal fun Project.silentWarnings() = getGradlePropForConfiguration(FLAG_SILENT_WARNINGS, false)
  internal fun Project.printBuildHealth() = getGradlePropForConfiguration(FLAG_PRINT_BUILD_HEALTH, false)
  internal fun Project.shouldClearArtifacts(): Boolean = getGradleOrSysProp(FLAG_CLEAR_ARTIFACTS, true)
  internal fun Project.getAllowedVariants() = getBuildPropForConfigurations(FLAG_VARIANTS_ANALYSIS)

  internal fun Project.cacheSize(default: Long): Long {
    return providers.systemProperty(FLAG_MAX_CACHE_SIZE)
      .forUseAtConfigurationTime()
      .map { userValue ->
        try {
          userValue.toLong()
        } catch (e: NumberFormatException) {
          throw GradleException("$userValue is not a valid cache size. Provide a long value", e)
        }
      }
      .getOrElse(default)
  }

  private fun Project.getGradleOrSysProp(name: String, default: Boolean): Boolean {
    val byGradle = getGradlePropForConfiguration(name, default)
    val bySys = getSysPropForConfiguration(name, default)
    return byGradle && bySys
  }

  private fun Project.getGradlePropForConfiguration(name: String, default: Boolean) =
    providers.gradleProperty(name)
      .forUseAtConfigurationTime()
      .getOrElse(default.toString())
      .toBoolean()

  private fun Project.getSysPropForConfiguration(name: String, default: Boolean) =
    providers.systemProperty(name)
      .forUseAtConfigurationTime()
      .getOrElse(default.toString())
      .toBoolean()

  private fun Project.getBuildPropForConfigurations(name: String) =
    providers.gradleProperty(name)
      .forUseAtConfigurationTime()
      .getOrElse("").split(',')
}
