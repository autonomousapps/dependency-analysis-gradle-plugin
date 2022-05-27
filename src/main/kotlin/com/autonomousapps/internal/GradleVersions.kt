package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {
  val current: GradleVersion = GradleVersion.current()
  val gradle74: GradleVersion = GradleVersion.version("7.4")
  val isAtLeastGradle74: Boolean = current >= gradle74
}
