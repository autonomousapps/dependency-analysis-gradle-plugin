@file:Suppress("UnstableApiUsage", "SameParameterValue")

package com.autonomousapps

import org.gradle.api.Project

const val FLAG_SILENT = "dependency.analysis.silent"
const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
const val FLAG_FAIL = "dependency.analysis.fail"
const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"
const val FLAG_CLEAR_ARTIFACTS = "dependency.analysis.clear.artifacts"
const val FLAG_AUTO_APPLY = "dependency.analysis.autoapply"
const val FLAG_MODEL_VERSION = "dependency.analysis.model.version"

internal fun shouldNotBeSilent() = getSysProp(FLAG_SILENT, true)
internal fun shouldFail() = getSysProp(FLAG_FAIL, false)

internal fun Project.shouldAnalyzeTests() = getSysPropForConfiguration(FLAG_TEST_ANALYSIS, true)
internal fun Project.shouldClearArtifacts() = getSysPropForConfiguration(FLAG_CLEAR_ARTIFACTS, true)
internal fun Project.shouldAutoApply() = getSysPropForConfiguration(FLAG_AUTO_APPLY, true)

private fun getSysProp(name: String, default: Boolean): Boolean {
  return System.getProperty(name, default.toString())!!.toBoolean()
}

private fun Project.getSysPropForConfiguration(name: String, default: Boolean) =
  providers.systemProperty(name)
    .forUseAtConfigurationTime()
    .getOrElse(default.toString())
    .toBoolean()
