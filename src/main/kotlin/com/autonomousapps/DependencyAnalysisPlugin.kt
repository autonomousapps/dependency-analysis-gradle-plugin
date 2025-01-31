// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.Flags.compatibility
import com.autonomousapps.internal.GradleVersions
import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.subplugin.ProjectPlugin
import com.autonomousapps.subplugin.RootPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger

internal const val TASK_GROUP_DEP = "dependency-analysis"

/** For use in contexts where a logger isn't easily available */
internal val PROJECT_LOGGER: Logger = getLogger<DependencyAnalysisPlugin>()

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

  internal companion object {
    const val ID = "com.autonomousapps.dependency-analysis"
  }

  override fun apply(project: Project): Unit = project.run {
    applyForRoot()
    applyForProject()
  }

  /** If this is the root project, apply configuration necessary for the root. */
  private fun Project.applyForRoot() {
    if (this == rootProject) {
      checkGradleVersion()
      checkAgpVersion()
      RootPlugin(this).apply()
    }
  }

  private fun checkGradleVersion() {
    check(GradleVersions.isAtLeastMinimum) {
      "Dependency Analysis Gradle Plugin requires Gradle ${GradleVersions.minGradleVersion.version} or higher. " +
        "Was ${GradleVersions.current.version}."
    }
  }

  /** Warn Android users if they're using an untested version of AGP. */
  private fun Project.checkAgpVersion() {
    val current = try {
      AgpVersion.current()
    } catch (_: Throwable) {
      logger.info("AGP not on classpath; assuming non-Android project")
      return
    }

    logger.debug("AgpVersion = $current")
    val compatibility = compatibility()
    if (compatibility != Flags.Compatibility.NONE && !current.isSupported() && this == rootProject) {
      val message = "The Dependency Analysis plugin is only known to work with versions of AGP between " +
        "${AgpVersion.AGP_MIN.version} and ${AgpVersion.AGP_MAX.version}. You are using ${current.version}. " +
        "Proceed at your own risk."
      @Suppress("KotlinConstantConditions")
      when (compatibility) {
        Flags.Compatibility.DEBUG -> logger.debug(message)
        Flags.Compatibility.WARN -> logger.warn(message)
        Flags.Compatibility.ERROR -> logger.error(message)
        Flags.Compatibility.NONE -> error("Not possible")
      }
    }
  }

  /** The following configuration is used by all projects, including the root. */
  private fun Project.applyForProject() {
    ProjectPlugin(this).apply()
  }
}
