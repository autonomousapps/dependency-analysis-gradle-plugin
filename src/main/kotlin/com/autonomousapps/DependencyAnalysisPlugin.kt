@file:Suppress("UnstableApiUsage")

package com.autonomousapps

import com.autonomousapps.internal.android.AgpVersion
import com.autonomousapps.subplugin.ProjectPlugin
import com.autonomousapps.subplugin.RootPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion

internal const val TASK_GROUP_DEP = "dependency-analysis"
internal const val TASK_GROUP_DEP_INTERNAL = "dependency-analysis-internal"

@Suppress("unused")
class DependencyAnalysisPlugin : Plugin<Project> {

  companion object {
    /** Minimum supported version of Gradle. For use by functional test suite. */
    @JvmField
    val MIN_GRADLE_VERSION: GradleVersion = GradleVersion.version("7.5")
  }

  override fun apply(project: Project): Unit = project.run {
    checkGradleVersion()
    checkAgpVersion()
    applyForRoot()
    checkPluginWasAppliedToRoot()
    applyForProject()
  }

  private fun checkGradleVersion() {
    val current = GradleVersion.current()
    check(current >= MIN_GRADLE_VERSION) {
      "Dependency Analysis Gradle Plugin requires Gradle ${MIN_GRADLE_VERSION.version} or higher. " +
        "Was ${current.version}."
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
    if (!current.isSupported() && this == rootProject) {
      logger.warn(
        "The Dependency Analysis plugin is only known to work with versions of AGP between " +
          "${AgpVersion.AGP_MIN.version} and ${AgpVersion.AGP_MAX.version}. You are using ${current.version}. Proceed" +
          " at your own risk."
      )
    }
  }

  /** If this is the root project, apply configuration necessary for the root. */
  private fun Project.applyForRoot() {
    if (this == rootProject) {
      RootPlugin(this).apply()
    }
  }

  /** Plugin _must_ be applied to the root for it to work. */
  private fun Project.checkPluginWasAppliedToRoot() {
    // "test" is the name of the dummy project that Kotlin DSL applies a plugin to when generating
    // script accessors
    if (getExtensionOrNull() == null && rootProject.name != "test") {
      throw GradleException("You must apply the plugin to the root project. Current project is $path")
    }
  }

  /** The following configuration is used by all projects, including the root. */
  private fun Project.applyForProject() {
    ProjectPlugin(this).apply()
  }
}
