// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {

  private val gradle74: GradleVersion = GradleVersion.version("7.4")
  private val gradle75: GradleVersion = GradleVersion.version("7.5")
  private val gradle82: GradleVersion = GradleVersion.version("8.2")
  private val gradle83: GradleVersion = GradleVersion.version("8.3")
  private val gradle85: GradleVersion = GradleVersion.version("8.5")
  private val gradle88: GradleVersion = GradleVersion.version("8.8")
  private val gradle811: GradleVersion = GradleVersion.version("8.11")

  /** Minimum supported version of Gradle. */
  @JvmField val minGradleVersion: GradleVersion = gradle74

  val current: GradleVersion = GradleVersion.current()

  val isAtLeastMinimum: Boolean = current >= minGradleVersion
  val isAtLeastGradle75: Boolean = current >= gradle75
  val isAtLeastGradle82: Boolean = current >= gradle82
  val isAtLeastGradle83: Boolean = current >= gradle83
  val isAtLeastGradle85: Boolean = current >= gradle85

  /** Minimum version of Gradle for [org.gradle.api.artifacts.ProjectDependency.getPath] */
  val isAtLeastGradle811: Boolean = current >= gradle811

  /**
   * Minimum version of Gradle for [BuildHealthPlugin][com.autonomousapps.BuildHealthPlugin], because it uses the new
   * lifecycle callbacks.
   */
  val isAtLeastGradle88: Boolean = current >= gradle88
}
