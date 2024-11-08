// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage", "SameParameterValue")

package com.autonomousapps

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.util.Locale

object Flags {

  // Deprecated
  internal const val FLAG_AUTO_APPLY = "dependency.analysis.autoapply"

  private const val FLAG_MAX_CACHE_SIZE = "dependency.analysis.cache.max"
  private const val FLAG_TEST_ANALYSIS = "dependency.analysis.test.analysis"
  private const val FLAG_PRINT_BUILD_HEALTH = "dependency.analysis.print.build.health"
  private const val FLAG_PROJECT_INCLUDES = "dependency.analysis.project.includes"

  // Used in tests
  internal const val FLAG_BYTECODE_LOGGING = "dependency.analysis.bytecode.logging"

  /**
   * Android build variant to not analyze i.e.
   *
   * ```
   * # gradle.properties
   * dependency.analysis.android.ignored.variants=release
   * ```
   */
  private const val FLAG_ANDROID_IGNORED_VARIANTS = "dependency.analysis.android.ignored.variants"

  private const val FLAG_DISABLE_COMPATIBILITY = "dependency.analysis.compatibility"

  internal fun Project.shouldAnalyzeTests() = getGradleOrSysProp(FLAG_TEST_ANALYSIS, true)

  internal fun Project.printBuildHealth() = getGradlePropForConfiguration(FLAG_PRINT_BUILD_HEALTH, false)
  internal fun Project.androidIgnoredVariants() = getGradlePropForConfiguration(
    FLAG_ANDROID_IGNORED_VARIANTS, ""
  ).split(",")

  internal fun Project.projectPathRegex(): Regex =
    getGradlePropForConfiguration(FLAG_PROJECT_INCLUDES, ".*").toRegex()

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

  /**
   * Passing `-Ddependency.analysis.bytecode.logging=true` will cause additional logs to print during bytecode analysis.
   *
   * `true` by default, meaning it suppresses console output (prints to debug stream).
   *
   * This is called from the runtime (not build time), so we use [System.getProperty] instead of
   * [project.providers.systemProperty][org.gradle.api.provider.ProviderFactory.systemProperty].
   */
  internal fun logBytecodeDebug(): Boolean {
    return !System.getProperty(FLAG_BYTECODE_LOGGING, "false").toBoolean()
  }

  internal fun Project.compatibility(): Compatibility {
    return getGradlePropForConfiguration(FLAG_DISABLE_COMPATIBILITY, Compatibility.WARN.name).let {
      @Suppress("DEPRECATION") val value = it.toUpperCase(Locale.US)
      Compatibility.values().find { it.name == value } ?: error(
        "Unrecognized value '$it' for 'dependency.analysis.compatibility' property. Allowed values are ${Compatibility.values()}"
      )
    }
  }

  private fun Project.getGradleOrSysProp(name: String, default: Boolean): Boolean {
    val byGradle = getGradlePropForConfiguration(name, default)
    val bySys = getSysPropForConfiguration(name, default)
    return byGradle && bySys
  }

  private fun Project.getGradlePropForConfiguration(name: String, default: String): String =
    providers.gradleProperty(name).getOrElse(default)

  private fun Project.getGradlePropForConfiguration(name: String, default: Boolean): Boolean =
    getGradlePropForConfiguration(name, default.toString()).toBoolean()

  private fun Project.getSysPropForConfiguration(name: String, default: Boolean) =
    providers.systemProperty(name)
      .getOrElse(default.toString())
      .toBoolean()

  internal enum class Compatibility {
    NONE,
    DEBUG,
    WARN,
    ERROR
  }
}
