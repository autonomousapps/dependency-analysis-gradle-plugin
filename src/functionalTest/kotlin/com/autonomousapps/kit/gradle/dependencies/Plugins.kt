// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.dependencies

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.gradle.Plugin

object Plugins {
  @JvmStatic val KOTLIN_VERSION: String = "1.9.22"

  @JvmStatic val dagpId: String = "com.autonomousapps.dependency-analysis"
  @JvmStatic val dependencyAnalysis: Plugin = Plugin(dagpId, AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION)
  @JvmStatic val dependencyAnalysisNoVersion: Plugin = Plugin(dagpId)

  @JvmStatic val androidApp: Plugin = Plugin("com.android.application")
  @JvmStatic val androidLib: Plugin = Plugin("com.android.library")
  @JvmStatic val gradleEnterprise: Plugin = Plugin("com.gradle.enterprise", "3.11.4")
  @JvmStatic val kapt: Plugin = Plugin("org.jetbrains.kotlin.kapt")
  @JvmStatic val kotlinAndroid: Plugin = Plugin("org.jetbrains.kotlin.android")
  @JvmStatic val kotlinNoVersion: Plugin = Plugin("org.jetbrains.kotlin.jvm")
  @JvmStatic val kotlinNoApply: Plugin = Plugin("org.jetbrains.kotlin.jvm", KOTLIN_VERSION, false)
  @JvmStatic val springBoot: Plugin = Plugin("org.springframework.boot", "2.7.14")
}
