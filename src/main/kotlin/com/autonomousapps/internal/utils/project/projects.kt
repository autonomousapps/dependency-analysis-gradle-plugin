package com.autonomousapps.internal.utils.project

import com.autonomousapps.internal.GradleVersions.isAtLeastGradle82
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.get

/** Get the buildPath of the current build from the root component of the resolution result. */
internal fun Project.buildPath(configuration: String): Provider<String> {
  return configurations[configuration].incoming.resolutionResult.let {
    if (isAtLeastGradle82) {
      it.rootComponent.map { root -> (root.id as ProjectComponentIdentifier).build.buildPath }
    } else {
      project.provider { @Suppress("DEPRECATION") (it.root.id as ProjectComponentIdentifier).build.name }
    }
  }
}
