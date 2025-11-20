// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.autonomousapps.services.GlobalDslService
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

/**
 * ```
 * // settings.gradle[.kts]
 * plugins {
 *   id("com.autonomousapps.build-health") version <<latest>>
 * }
 * ```
 */
public abstract class BuildHealthPlugin : Plugin<Settings> {

  internal companion object {
    const val ID = "com.autonomousapps.build-health"
  }

  override fun apply(target: Settings): Unit = target.run {
    // Create extension
    DependencyAnalysisExtension.of(this)

    // Register service
    GlobalDslService.of(target.gradle).get().setRegisteredOnSettings()

    gradle.lifecycle.beforeProject { p ->
      p.pluginManager.apply(DependencyAnalysisPlugin.ID)
    }

    // The version catalog extension won't be available until AFTER this plugin has configured itself, so register a
    // callback for after the project configuration has run.
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1441
    // https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1603
    gradle.lifecycle.afterProject { p ->
      GlobalDslService.of(p).get().withVersionCatalogs(p)
    }
  }
}
