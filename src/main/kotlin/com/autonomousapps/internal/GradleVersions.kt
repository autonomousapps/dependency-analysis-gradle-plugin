// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.BuildConfig.GRADLE_MIN_VERSION
import com.autonomousapps.internal.BuildConfig.GRADLE_MAX_VERSION
import org.gradle.util.GradleVersion

internal object GradleVersions {

  /** Minimum supported version of Gradle. */
  @JvmField val minGradleVersion: GradleVersion = GradleVersion.version(GRADLE_MIN_VERSION)

  /** Max supported version of Gradle. */
  @JvmField val maxGradleVersion: GradleVersion = GradleVersion.version(GRADLE_MAX_VERSION)

  /** Version of Gradle we're building against. */
  @JvmField val current: GradleVersion = GradleVersion.current()

  private val gradle811: GradleVersion = GradleVersion.version("8.11.1")
  private val gradle900: GradleVersion = GradleVersion.version("9.0.0")
  private val gradle910: GradleVersion = GradleVersion.version("9.1.0")
  private val gradle940: GradleVersion = GradleVersion.version("9.4.0")
  private val gradle970: GradleVersion = GradleVersion.version("9.7.0-rc-1")

  val isAtLeastMinimum: Boolean = current >= minGradleVersion

  /** Minimum version of Gradle for [org.gradle.api.artifacts.ProjectDependency.getPath] */
  val isAtLeastGradle811: Boolean = current >= gradle811

  val isAtLeastGradle900: Boolean = current >= gradle900
  val isAtLeastGradle910: Boolean = current >= gradle910
  val isAtLeastGradle940: Boolean = current >= gradle940
  val isAtLeastGradle970: Boolean = current >= gradle970
}
