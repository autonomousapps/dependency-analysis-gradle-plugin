// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils.project

import com.autonomousapps.internal.GradleVersions
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.internal.build.BuildState

/** Get the buildPath of the current build from the root component of the resolution result. */
internal fun Project.buildPath(configurationName: String): Provider<String> {
  return buildPath(configurations.named(configurationName))
}

// TODO(tsr): update to use project.gradle.buildPath with Gradle 9.1.0
internal fun Project.buildPath(configuration: Configuration): Provider<String> {
  return if (GradleVersions.isAtLeastGradle900) {
    project.provider { getIdentityPath() }
  } else {
    configuration.incoming.resolutionResult.let {
      it.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
    }
  }
}

// TODO(tsr): update to use project.gradle.buildPath with Gradle 9.1.0
internal fun Project.buildPath(configuration: NamedDomainObjectProvider<Configuration>): Provider<String> {
  return if (GradleVersions.isAtLeastGradle900) {
    project.provider { getIdentityPath() }
  } else {
    configuration.flatMap { c ->
      c.incoming.resolutionResult.let { result ->
        result.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
      }
    }
  }
}

/**
 * @see <a href="https://github.com/gradle/gradle/pull/34007">Gradle PR #34007</a>
 * @see <a href="https://github.com/JetBrains/kotlin/blob/fb3d7ebc67466a2e1a30217337fc76df96d4732e/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/utils/CurrentBuildIdentifier.kt#L15-L17">KGP</a>
 */
internal fun Project.getIdentityPath(): String =
  (project as ProjectInternal).services.get(BuildState::class.java).identityPath.toString()
