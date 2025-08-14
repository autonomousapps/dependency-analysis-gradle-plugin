// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

/**
 * Utilities for working with Gradle Kotlin DSL type-safe accessors.
 */
internal object TypeSafeAccessorUtils {

  /**
   * Converts type-safe project accessor patterns to canonical project paths.
   * E.g., "projects.myModule" -> ":my-module"
   *       "projects.nested.subModule" -> ":nested:sub-module"
   */
  fun normalizeTypeSafeProjectAccessor(identifier: String): String {
    if (!isTypeSafeProjectAccessor(identifier)) {
      return identifier
    }

    // Remove "projects" prefix and convert camelCase to kebab-case project path
    val projectPath = identifier.removePrefix("projects")
      .split('.')
      .filter { it.isNotEmpty() }
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
  fun isTypeSafeProjectAccessor(identifier: String): Boolean {
    return identifier.startsWith("projects.") || identifier == "projects"
  }
}
