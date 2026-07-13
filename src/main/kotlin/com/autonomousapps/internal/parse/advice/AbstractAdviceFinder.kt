// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse.advice

import com.autonomousapps.internal.cash.grammar.kotlindsl.model.DependencyDeclaration
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates

internal abstract class AbstractAdviceFinder(
  protected val advice: Set<Advice>,
  protected val reversedDependencyMap: (String) -> String,
) : AdviceFinder {

  protected fun Advice.matchesIdentifier(identifier: String): Boolean {
    return coordinates.gav() == identifier || coordinates.identifier == identifier
  }

  /**
   * Normalizes type-safe project accessors to standard project path format.
   * E.g., "projects.myModule" -> ":my-module"
   * But "projects.common.viewmodels" -> ":common:viewmodels" (dots become colons)
   */
  protected fun normalizeTypeSafeProjectAccessor(identifier: String): String {
    if (!identifier.startsWith("projects.")) {
      return identifier
    }

    // Convert "projects.myModule" -> ":my-module"
    // Handle camelCase to kebab-case conversion, but also handle dots as path separators
    val projectPath = identifier.removePrefix("projects.")

    // Replace dots with colons first (for submodules like "common.viewmodels" -> "common:viewmodels")
    val withColons = projectPath.replace(".", ":")

    // Then handle camelCase to kebab-case conversion for each segment
    val segments = withColons.split(":")
    val normalizedSegments = segments.map { segment ->
      segment.replace(Regex("([a-z])([A-Z])")) { matchResult ->
        "${matchResult.groupValues[1]}-${matchResult.groupValues[2].lowercase()}"
      }
    }

    return ":${normalizedSegments.joinToString(":")}"
  }

  protected fun Advice.matchesType(dependencyDeclaration: DependencyDeclaration, rawIdentifier: String): Boolean {
    // Special handling for type-safe project accessors that might be misclassified by the parser
    // Type-safe project accessors like "projects.myModule" might be parsed as MODULE type
    // instead of PROJECT type by the ANTLR parser.
    if (rawIdentifier.startsWith("projects.") && coordinates is ProjectCoordinates) {
      return true
    }

    // Standard type matching
    return when (dependencyDeclaration.type) {
      DependencyDeclaration.Type.MODULE -> coordinates is ModuleCoordinates
      DependencyDeclaration.Type.PROJECT -> coordinates is ProjectCoordinates

      // TODO(tsr): I think returning false is fine. We can't replace these.
      DependencyDeclaration.Type.GRADLE_DISTRIBUTION -> false
      DependencyDeclaration.Type.FILE -> false
      DependencyDeclaration.Type.FILES -> false
      DependencyDeclaration.Type.FILE_TREE -> false
    }
  }

  protected fun Advice.matchesCapabilities(dependencyDeclaration: DependencyDeclaration): Boolean {
    return when (dependencyDeclaration.capability) {
      DependencyDeclaration.Capability.DEFAULT -> coordinates.gradleVariantIdentification.capabilities.isEmpty()

      DependencyDeclaration.Capability.ENFORCED_PLATFORM -> {
        coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(GradleVariantIdentification.ENFORCED_PLATFORM) }
      }

      DependencyDeclaration.Capability.PLATFORM -> {
        coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(GradleVariantIdentification.PLATFORM) }
      }

      DependencyDeclaration.Capability.TEST_FIXTURES -> {
        coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(GradleVariantIdentification.TEST_FIXTURES) }
      }
    }
  }
}
