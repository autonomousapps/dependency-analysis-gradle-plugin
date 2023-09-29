package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {

  private val current: GradleVersion = GradleVersion.current()
  private val gradle74: GradleVersion = GradleVersion.version("7.4")
  private val gradle82: GradleVersion = GradleVersion.version("8.2")

  val isAtLeastGradle74: Boolean = current >= gradle74
  val isAtLeastGradle82: Boolean = current >= gradle82
}
