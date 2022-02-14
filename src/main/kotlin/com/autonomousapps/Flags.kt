@file:Suppress("UnstableApiUsage", "SameParameterValue")

package com.autonomousapps

import org.gradle.api.GradleException
import org.gradle.api.Project

@Suppress("DEPRECATION") // forUseAtConfigurationTime()
object Flags {

  internal const val FLAG_CLEAR_ARTIFACTS = "dependency.analysis.clear.artifacts"
  internal const val FLAG_SILENT_WARNINGS = "dependency.analysis.warnings.silent"

  private const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
  private const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"
  private const val FLAG_AUTO_APPLY = "dependency.analysis.autoapply"

  internal fun Project.shouldAnalyzeTests() = getSysPropForConfiguration(FLAG_TEST_ANALYSIS, true)
  internal fun Project.shouldAutoApply() = getSysPropForConfiguration(FLAG_AUTO_APPLY, true)
  internal fun Project.silentWarnings() = getGradlePropForConfiguration(FLAG_SILENT_WARNINGS, false)

  internal fun Project.shouldClearArtifacts(): Boolean {
    val byGradle = getGradlePropForConfiguration(FLAG_CLEAR_ARTIFACTS, true)
    val bySys = getSysPropForConfiguration(FLAG_CLEAR_ARTIFACTS, true)
    return byGradle && bySys
  }

  internal fun Project.cacheSize(default: Long): Long {
    return providers.systemProperty(FLAG_MAX_CACHE_SIZE)
      .map { userValue ->
        try {
          userValue.toLong()
        } catch (e: NumberFormatException) {
          throw GradleException("$userValue is not a valid cache size. Provide a long value", e)
        }
      }
      .getOrElse(default)
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
}
