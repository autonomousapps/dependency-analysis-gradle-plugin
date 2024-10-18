// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import org.gradle.util.GradleVersion

internal object GradleVersions {

  private val gradle80: GradleVersion = GradleVersion.version("8.0")
  private val gradle81: GradleVersion = GradleVersion.version("8.1")
  private val gradle82: GradleVersion = GradleVersion.version("8.2")
  private val gradle83: GradleVersion = GradleVersion.version("8.3")
  private val gradle84: GradleVersion = GradleVersion.version("8.4")
  private val gradle85: GradleVersion = GradleVersion.version("8.5")
  private val gradle86: GradleVersion = GradleVersion.version("8.6")
  private val gradle87: GradleVersion = GradleVersion.version("8.7")
  private val gradle88: GradleVersion = GradleVersion.version("8.8")
  private val gradle89: GradleVersion = GradleVersion.version("8.9")

  /** Minimum supported version of Gradle. */
  @JvmField val minGradleVersion: GradleVersion = gradle80

  val current: GradleVersion = GradleVersion.current()

  val isAtLeastMinimum: Boolean = current >= minGradleVersion
  val isAtLeastGradle82: Boolean = current >= gradle82
  val isAtLeastGradle85: Boolean = current >= gradle85

  /**
   * Minimum version of Gradle for [BuildHealthPlugin][com.autonomousapps.BuildHealthPlugin], because it uses the new
   * lifecycle callbacks.
   */
  val isAtLeastGradle88: Boolean = current >= gradle88
}
