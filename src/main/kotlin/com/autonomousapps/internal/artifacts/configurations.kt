@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.artifacts

import com.autonomousapps.internal.GradleVersions
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Creates a "dependency scope"-type configuration, which we can think of as a _bucket_ for declaring dependencies. See
 * also [resolvableConfiguration] and [consumableConfiguration].
 */
internal fun Project.dependencyScopeConfiguration(configurationName: String): NamedDomainObjectProvider<out Configuration> {
  return if (GradleVersions.isAtLeastGradle84) {
    configurations.dependencyScope(configurationName)
  } else {
    configurations.register(configurationName) {
      isCanBeResolved = false
      isCanBeConsumed = true
      isVisible = false
    }
  }
}

/**
 * Creates a "resolvable"-type configuration, which can be thought of as the method by which projects "resolve" the
 * dependencies that they declare on the [dependencyScopeConfiguration] configurations.
 */
internal fun Project.resolvableConfiguration(
  configurationName: String,
  dependencyScopeConfiguration: Configuration,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
  return if (GradleVersions.isAtLeastGradle84) {
    configurations.resolvable(configurationName) {
      extendsFrom(dependencyScopeConfiguration)
      configureAction.execute(this)
    }
  } else {
    configurations.register(configurationName) {
      isCanBeResolved = true
      isCanBeConsumed = false
      isVisible = false

      extendsFrom(dependencyScopeConfiguration)

      configureAction.execute(this)
    }
  }
}

/**
 * Creates a "consumable"-type configuration, which can be thought of as the method by which projects export artifacts
 * to consumer projects, which have declared a dependency on _this_ project using the [dependencyScopeConfiguration]
 * configuration (which may be `null` for this project).
 */
internal fun Project.consumableConfiguration(
  configurationName: String,
  dependencyScopeConfiguration: Configuration? = null,
  configureAction: Action<in Configuration>,
): NamedDomainObjectProvider<out Configuration> {
  return if (GradleVersions.isAtLeastGradle84) {
    configurations.consumable(configurationName) {
      dependencyScopeConfiguration?.let { extendsFrom(it) }
      configureAction.execute(this)
    }
  } else {
    configurations.register(configurationName) {
      isCanBeConsumed = true
      isCanBeResolved = false
      isVisible = false

      dependencyScopeConfiguration?.let { extendsFrom(it) }

      configureAction.execute(this)
    }
  }
}
