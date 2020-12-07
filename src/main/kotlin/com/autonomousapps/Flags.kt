@file:Suppress("UnstableApiUsage", "SameParameterValue")

package com.autonomousapps

import org.gradle.api.Project
import org.gradle.util.GradleVersion

const val FLAG_SILENT = "dependency.analysis.silent"
const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
const val FLAG_FAIL = "dependency.analysis.fail"
const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"
const val FLAG_CLEAR_ARTIFACTS = "dependency.analysis.clear.artifacts"

internal fun shouldNotBeSilent() = getSysProp(FLAG_SILENT, true)
internal fun shouldFail() = getSysProp(FLAG_FAIL, false)

internal fun Project.shouldAnalyzeTests() = getSysPropForConfiguration(FLAG_TEST_ANALYSIS, true)
internal fun Project.shouldClearArtifacts() = getSysPropForConfiguration(FLAG_CLEAR_ARTIFACTS, true)

private fun getSysProp(name: String, default: Boolean): Boolean {
  return System.getProperty(name, default.toString())!!.toBoolean()
}

// getOrElse() never returns null!
@Suppress("PlatformExtensionReceiverOfInline")
private fun Project.getSysPropForConfiguration(name: String, default: Boolean): Boolean {
  return providers.systemProperty(name).run {
    if (GradleVersion.current() < GradleVersion.version("6.5")) this
    else forUseAtConfigurationTime()
  }.getOrElse(default.toString()).toBoolean()
}
