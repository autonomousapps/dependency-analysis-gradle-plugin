package com.autonomousapps.kit.gradle.dependencies

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.gradle.Plugin

class PluginProvider(
  kotlinVersion: String,
  androidVersion: String? = null,
) {

  private val pluginUnderTestVersion = AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION
  private val springBootVersion = "2.7.14"

  val buildHealthId: String = "com.autonomousapps.build-health"
  val buildHealth: Plugin = Plugin(buildHealthId, pluginUnderTestVersion)

  val dagpId: String = "com.autonomousapps.dependency-analysis"
  val dependencyAnalysis: Plugin = Plugin(dagpId, pluginUnderTestVersion)
  val dependencyAnalysisNoVersion: Plugin = Plugin(dagpId)

  val gradleDependenciesSorter: Plugin = Plugin("com.squareup.sort-dependencies", "0.10")

  val androidAppId: String = "com.android.application"
  val androidLibId: String = "com.android.library"
  val androidApp: Plugin = Plugin(androidAppId, androidVersion)
  val androidAppNoVersion: Plugin = Plugin(androidAppId)
  val androidLibNoVersion: Plugin = Plugin(androidLibId)

  val kotlinJvm: Plugin = Plugin("org.jetbrains.kotlin.jvm", kotlinVersion)
  val kotlinJvmNoApply: Plugin = Plugin("org.jetbrains.kotlin.jvm", kotlinVersion, false)
  val kotlinAndroid: Plugin = Plugin("org.jetbrains.kotlin.android", kotlinVersion)
  val kotlinAndroidNoVersion: Plugin = Plugin("org.jetbrains.kotlin.android")
  val kotlinJvmNoVersion: Plugin = Plugin("org.jetbrains.kotlin.jvm")
  val kotlinKaptNoVersion: Plugin = Plugin("org.jetbrains.kotlin.kapt")

  val springBoot: Plugin = Plugin("org.springframework.boot", springBootVersion)

  /*
   * Core plugins. This layer exists for ease of writing test fixtures.
   */

  val javaLibrary: Plugin = Plugin.javaLibrary
  val javaTestFixtures: Plugin = Plugin.javaTestFixtures
}
