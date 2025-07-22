// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.artifacts

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.util.GradleVersion

private val current: GradleVersion = GradleVersion.current()
private val gradle85: GradleVersion = GradleVersion.version("8.5")
private val isAtLeastGradle85: Boolean = current >= gradle85

/**
 * Creates a "dependency scope"-type configuration, which we can think of as a _bucket_ for declaring dependencies. See
 * also [resolvableConfiguration] and [consumableConfiguration].
 */
public fun Project.dependencyScopeConfiguration(configurationName: String): NamedDomainObjectProvider<out Configuration> {
  return if (isAtLeastGradle85) {
    configurations.dependencyScope(configurationName)
  } else {
    configurations.register(configurationName) { c ->
      c.isCanBeResolved = false
      c.isCanBeConsumed = true
      c.isVisible = false
    }
  }
}

/**
 * Creates a "resolvable"-type configuration, which can be thought of as the method by which projects "resolve" the
 * dependencies that they declare on the [dependencyScopeConfiguration] configurations.
 */
public fun Project.resolvableConfiguration(
  configurationName: String,
  dependencyScopeConfiguration: NamedDomainObjectProvider<out Configuration>,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
  return if (isAtLeastGradle85) {
    configurations.resolvable(configurationName) { c ->
      c.extendsFrom(dependencyScopeConfiguration.get())
      configureAction.execute(c)
    }
  } else {
    configurations.register(configurationName) { c ->
      c.isCanBeResolved = true
      c.isCanBeConsumed = false
      c.isVisible = false

      c.extendsFrom(dependencyScopeConfiguration.get())
      configureAction.execute(c)
    }
  }
}

/**
 * Creates a "consumable"-type configuration, which can be thought of as the method by which projects export artifacts
 * to consumer projects, which have declared a dependency on _this_ project using the [dependencyScopeConfiguration]
 * configuration (which may be `null` for this project).
 */
public fun Project.consumableConfiguration(
  configurationName: String,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
  return if (isAtLeastGradle85) {
    configurations.consumable(configurationName) { c ->
      configureAction.execute(c)
    }
  } else {
    configurations.register(configurationName) { c ->
      c.isCanBeConsumed = true
      c.isCanBeResolved = false
      c.isVisible = false

      configureAction.execute(c)
    }
  }
}
