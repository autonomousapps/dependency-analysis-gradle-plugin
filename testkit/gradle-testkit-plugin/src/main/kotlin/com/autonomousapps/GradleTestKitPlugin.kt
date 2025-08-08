// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * ```
 * plugins {
 *   id("com.autonomousapps.testkit")
 * }
 * ```
 */
public class GradleTestKitPlugin : Plugin<Project> {
  override fun apply(target: Project): Unit = target.run {
    val extension = GradleTestKitSupportExtension.create(this)

    // Plugin projects get this automatically
    pluginManager.withPlugin("java-gradle-plugin") {
      extension.registerFunctionalTest()
    }
  }
}
