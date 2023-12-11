package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {

  private val gradle74: GradleVersion = GradleVersion.version("7.4")
  private val gradle75: GradleVersion = GradleVersion.version("7.5")
  private val gradle82: GradleVersion = GradleVersion.version("8.2")
  private val gradle84: GradleVersion = GradleVersion.version("8.4")

  /** Minimum supported version of Gradle. */
  @JvmField val minGradleVersion: GradleVersion = gradle74
  // @JvmField val minGradleVersion: GradleVersion = GradleVersion.version("7.5")

  val current: GradleVersion = GradleVersion.current()

  val isAtLeastMinimum: Boolean = current >= minGradleVersion
  val isAtLeastGradle74: Boolean = current >= gradle74
  val isAtLeastGradle75: Boolean = current >= gradle75
  val isAtLeastGradle82: Boolean = current >= gradle82
  val isAtLeastGradle84: Boolean = current >= gradle84
}
