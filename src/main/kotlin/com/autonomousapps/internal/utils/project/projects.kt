// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.project

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider

/** Get the buildPath of the current build from the root component of the resolution result. */
internal fun Project.buildPath(configurationName: String): Provider<String> {
  return buildPath(configurations.named(configurationName))
}

// TODO(tsr): update to use project.gradle.buildPath with Gradle 9.1.0
internal fun Project.buildPath(configuration: Configuration): Provider<String> {
  return configuration.incoming.resolutionResult.let {
    it.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
  }
}

// TODO(tsr): update to use project.gradle.buildPath with Gradle 9.1.0
internal fun Project.buildPath(configuration: NamedDomainObjectProvider<Configuration>): Provider<String> {
  return configuration.flatMap { c ->
    c.incoming.resolutionResult.let { result ->
      result.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
    }
  }
}
