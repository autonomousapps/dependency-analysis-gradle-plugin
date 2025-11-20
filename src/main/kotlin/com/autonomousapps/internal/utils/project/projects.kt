// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.project

import com.autonomousapps.internal.GradleVersions
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider

/** Get the buildPath of the current build. */
internal fun Project.buildPath(configurationName: String): Provider<String> {
  return if (GradleVersions.isAtLeastGradle910) {
    provider { gradle.buildPath }
  } else {
    buildPath(configurations.named(configurationName))
  }
}

/** Get the buildPath of the current build. */
internal fun Project.buildPath(configuration: NamedDomainObjectProvider<Configuration>): Provider<String> {
  return if (GradleVersions.isAtLeastGradle910) {
    provider { gradle.buildPath }
  } else {
    configuration.flatMap { c ->
      c.incoming.resolutionResult.let { result ->
        result.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
      }
    }
  }
}
