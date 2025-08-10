// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.project

import com.autonomousapps.internal.GradleVersions.isAtLeastGradle82
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider

/** Get the buildPath of the current build from the root component of the resolution result. */
internal fun Project.buildPath(configurationName: String): Provider<String> {
  return buildPath(configurations.named(configurationName))
}

internal fun Project.buildPath(configuration: Configuration): Provider<String> {
  return configuration.incoming.resolutionResult.let {
    if (isAtLeastGradle82) {
      it.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
    } else {
      project.provider { @Suppress("DEPRECATION") (it.root.id as ProjectComponentIdentifier).build.name }
    }
  }
}

internal fun Project.buildPath(configuration: NamedDomainObjectProvider<Configuration>): Provider<String> {
  return configuration.flatMap { c ->
    c.incoming.resolutionResult.let { result ->
      if (isAtLeastGradle82) {
        result.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
      } else {
        project.provider { @Suppress("DEPRECATION") (result.root.id as ProjectComponentIdentifier).build.name }
      }
    }
  }
}
