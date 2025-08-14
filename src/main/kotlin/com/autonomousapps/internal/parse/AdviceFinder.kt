// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import com.autonomousapps.internal.cash.grammar.kotlindsl.model.DependencyDeclaration
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates

// TODO(tsr): write unit tests
internal class AdviceFinder(
  private val advice: Set<Advice>,
  private val reversedDependencyMap: (String) -> String,
) {

  fun findAdvice(dependencyDeclaration: DependencyDeclaration): Advice? {
    val rawIdentifier = dependencyDeclaration.identifier.path.removeSurrounding("\"")
    val identifier = reversedDependencyMap(rawIdentifier)
    
    // Handle type-safe project accessors (e.g., "projects.myModule" -> ":my-module")
    val normalizedIdentifier = normalizeTypeSafeProjectAccessor(identifier)

    return advice.find {
      // First match on GAV/identifier
      (it.matchesIdentifier(identifier) || it.matchesIdentifier(normalizedIdentifier))
        // Then match on configuration
        && it.matchesConfiguration(dependencyDeclaration)
        // Then match on type (project, module, etc) - enhanced for type-safe accessors
        && it.matchesTypeEnhanced(dependencyDeclaration, rawIdentifier)
        // Then match on capabilities
        && it.matchesCapabilities(dependencyDeclaration)
    }
  }

  private fun Advice.matchesIdentifier(identifier: String): Boolean {
    return coordinates.gav() == identifier || coordinates.identifier == identifier
  }

  private fun Advice.matchesConfiguration(dependencyDeclaration: DependencyDeclaration): Boolean {
    return fromConfiguration == dependencyDeclaration.configuration
  }

  private fun Advice.matchesType(dependencyDeclaration: DependencyDeclaration): Boolean {
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

  /**
   * Enhanced type matching that also considers type-safe project accessor patterns.
   * Type-safe accessors like "projects.myModule" might be parsed as MODULE type
   * instead of PROJECT type, so we need additional logic to handle them.
   */
  private fun Advice.matchesTypeEnhanced(dependencyDeclaration: DependencyDeclaration, rawIdentifier: String): Boolean {
    return when (dependencyDeclaration.type) {
      DependencyDeclaration.Type.MODULE -> {
        // Check if this coordinates is actually a project but was parsed as module due to type-safe accessor
        if (coordinates is ProjectCoordinates && isTypeSafeProjectAccessor(rawIdentifier)) {
          true
        } else {
          coordinates is ModuleCoordinates
        }
      }
      DependencyDeclaration.Type.PROJECT -> coordinates is ProjectCoordinates

      // TODO(tsr): I think returning false is fine. We can't replace these.
      DependencyDeclaration.Type.GRADLE_DISTRIBUTION -> false
      DependencyDeclaration.Type.FILE -> false
      DependencyDeclaration.Type.FILES -> false
      DependencyDeclaration.Type.FILE_TREE -> false
    }
  }

  private fun Advice.matchesCapabilities(dependencyDeclaration: DependencyDeclaration): Boolean {
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

  /**
   * Converts type-safe project accessor patterns to canonical project paths.
   * E.g., "projects.myModule" -> ":my-module"
   *       "projects.nested.subModule" -> ":nested:sub-module"
   */
  private fun normalizeTypeSafeProjectAccessor(identifier: String): String {
    if (!isTypeSafeProjectAccessor(identifier)) {
      return identifier
    }

    // Remove "projects" prefix and convert camelCase to kebab-case project path
    val projectPath = identifier.removePrefix("projects")
      .split('.')
      .joinToString(":") { segment ->
        // Convert camelCase to kebab-case
        segment.replace(Regex("([a-z0-9])([A-Z])")) { matchResult ->
          "${matchResult.groupValues[1]}-${matchResult.groupValues[2].lowercase()}"
        }
      }

    return if (projectPath.startsWith(":")) projectPath else ":$projectPath"
  }

  /**
   * Checks if the given identifier is a type-safe project accessor pattern.
   */
  private fun isTypeSafeProjectAccessor(identifier: String): Boolean {
    return identifier.startsWith("projects.") || identifier == "projects"
  }
}
