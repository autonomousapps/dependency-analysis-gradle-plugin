// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.dependencies

import com.autonomousapps.kit.gradle.Plugin

object Plugins {

  @JvmStatic val KOTLIN_VERSION: String = System.getProperty("com.autonomousapps.test.versions.kotlin")
  @JvmStatic val KOTLIN_LATER_VERSION: String = System.getProperty("com.autonomousapps.test.versions.kotlin.later")

  private val provider = PluginProvider(
    kotlinVersion = KOTLIN_VERSION
  )

  @JvmStatic val buildHealthId: String = provider.buildHealthId
  @JvmStatic val buildHealth: Plugin = provider.buildHealth

  @JvmStatic val dagpId: String = provider.dagpId
  @JvmStatic val dependencyAnalysis: Plugin = provider.dependencyAnalysis
  @JvmStatic val dependencyAnalysisNoVersion: Plugin = provider.dependencyAnalysisNoVersion

  @JvmStatic val androidAppId: String = provider.androidAppId
  @JvmStatic val androidApp: Plugin = provider.androidAppNoVersion
  @JvmStatic val androidLib: Plugin = provider.androidLibNoVersion
  @JvmStatic val androidKmpLibNoVersion: Plugin = provider.androidKmpLibNoVersion
  @JvmStatic val androidTest: Plugin = provider.androidTestNoVersion

  @JvmStatic val javaTestFixtures: Plugin = provider.javaTestFixtures

  @JvmStatic val kotlinJvm: Plugin = provider.kotlinJvm
  @JvmStatic val kotlinJvmNoApply: Plugin = provider.kotlinJvmNoApply
  @JvmStatic val kotlinAndroid: Plugin = provider.kotlinAndroid
  @JvmStatic val kotlinAndroidNoVersion: Plugin = provider.kotlinAndroidNoVersion
  @JvmStatic val kotlinJvmNoVersion: Plugin = provider.kotlinJvmNoVersion
  @JvmStatic val kotlinMultiplatformNoApply: Plugin = provider.kotlinMultiplatformNoApply
  @JvmStatic val kotlinMultiplatformNoVersion: Plugin = provider.kotlinMultiplatformNoVersion
  @JvmStatic val kotlinKaptNoVersion: Plugin = provider.kotlinKaptNoVersion

  @JvmStatic val springBoot: Plugin = provider.springBoot
}
