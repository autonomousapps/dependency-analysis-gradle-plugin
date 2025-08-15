// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

/**
 * Utilities for working with Gradle Kotlin DSL type-safe accessors.
 */
internal object TypeSafeAccessorUtils {

  /**
   * Pattern to match type-safe accessors with parentheses (after preprocessing).
   * E.g., "implementation(projects.myModule)" or "api(libs.someLibrary)"
   */
  val ACCESSOR_WITH_PARENS = Regex("""(\w+)\(((?:projects|libs)\.[\w.]+)\)""")

  /**
   * Pattern to match type-safe accessors without parentheses (original syntax).
   * E.g., "implementation projects.myModule" or "api libs.someLibrary"
   */
  val ACCESSOR_WITHOUT_PARENS = Regex("""(\w+)\s+((?:projects|libs)\.[\w.]+)\b""")

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

  /**
   * Information about a type-safe accessor extracted from text.
   */
  data class AccessorInfo(
    val configuration: String,
    val accessor: String,
    val hasParentheses: Boolean
  )

  /**
   * Extracts accessor information from the given text.
   * Returns null if no type-safe accessor is found.
   */
  fun extractAccessorInfo(text: String): AccessorInfo? {
    // Try with parentheses first
    ACCESSOR_WITH_PARENS.find(text.trim())?.let { match ->
      return AccessorInfo(
        configuration = match.groupValues[1],
        accessor = match.groupValues[2],
        hasParentheses = true
      )
    }
    
    // Try without parentheses
    ACCESSOR_WITHOUT_PARENS.find(text.trim())?.let { match ->
      return AccessorInfo(
        configuration = match.groupValues[1],
        accessor = match.groupValues[2],
        hasParentheses = false
      )
    }
    
    return null
  }
}
