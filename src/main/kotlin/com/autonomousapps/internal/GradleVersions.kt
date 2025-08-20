// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {

  private val gradle811: GradleVersion = GradleVersion.version("8.11")
  private val gradle900: GradleVersion = GradleVersion.version("9.0.0")

  /** Minimum supported version of Gradle. */
  @JvmField val minGradleVersion: GradleVersion = gradle811

  val current: GradleVersion = GradleVersion.current()

  val isAtLeastMinimum: Boolean = current >= minGradleVersion

  /** Minimum version of Gradle for [org.gradle.api.artifacts.ProjectDependency.getPath] */
  val isAtLeastGradle811: Boolean = current >= gradle811

  val isAtLeastGradle900: Boolean = current >= gradle900
}
