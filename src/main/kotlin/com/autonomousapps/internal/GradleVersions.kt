// Copyright (c) 2025. Tony Robalik.
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

  val isAtLeastMinimum: Boolean = current >= minGradleVersion

  /** Minimum version of Gradle for [org.gradle.api.artifacts.ProjectDependency.getPath] */
  val isAtLeastGradle811: Boolean = current >= gradle811

  val isAtLeastGradle900: Boolean = current >= gradle900
}
