package com.autonomousapps

import com.autonomousapps.services.GlobalDslService
import com.autonomousapps.subplugin.DEPENDENCY_ANALYSIS_PLUGIN
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
class BuildHealthPlugin : Plugin<Settings> {

  internal companion object {
    const val ID = "com.autonomousapps.build-health"
  }

  override fun apply(target: Settings): Unit = target.run {
    GlobalDslService.of(target.gradle).apply {
      get().apply {
        setRegisteredOnSettings()
      }
    }

    gradle.lifecycle.beforeProject {
      pluginManager.apply(DEPENDENCY_ANALYSIS_PLUGIN)
    }
  }
}
