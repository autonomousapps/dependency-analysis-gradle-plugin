package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {
  val current: GradleVersion = GradleVersion.current()
  val gradle74: GradleVersion = GradleVersion.version("7.4")
  val gradle82: GradleVersion = GradleVersion.version("8.2")
  val isAtLeastGradle74: Boolean = current >= gradle74
  val isAtLeastGradle82: Boolean = current >= gradle82
}
