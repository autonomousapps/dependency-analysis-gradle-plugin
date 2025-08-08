package com.autonomousapps

import com.autonomousapps.internal.GradleVersions
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
    if (!GradleVersions.isAtLeastGradle88) {
      error("'$ID' requires Gradle 8.8 or higher.")
    }

    // Create extension
    DependencyAnalysisExtension.of(this)

    // Register service
    GlobalDslService.of(target.gradle).apply {
      get().apply {
        setRegisteredOnSettings()
      }
    }

    gradle.lifecycle.beforeProject {
      pluginManager.apply(DependencyAnalysisPlugin.ID)
    }
  }
}
