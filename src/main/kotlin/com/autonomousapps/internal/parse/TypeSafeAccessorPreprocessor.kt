// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.parse

import java.io.InputStream

/**
 * Handles preprocessing of Gradle build scripts to normalize type-safe accessor syntax.
 * Converts non-parentheses syntax to parentheses syntax for parser compatibility.
 */
internal class TypeSafeAccessorPreprocessor {

  /**
   * Result of preprocessing containing both transformed content and style metadata.
   */
  data class PreprocessingResult(
    val content: String,
    val originalContent: String,
    val styleMap: Map<String, Boolean> // accessor -> hasParentheses
  )

  companion object {
    // Flexible pattern that matches Gradle configuration naming conventions
    private val CONFIGURATION_PATTERN = """(\b\w*(?:implementation|Implementation|api|Api|compileOnly|CompileOnly|runtimeOnly|RuntimeOnly|testImplementation|TestImplementation|testCompileOnly|TestCompileOnly|testRuntimeOnly|TestRuntimeOnly|androidTestImplementation|AndroidTestImplementation|debugImplementation|DebugImplementation|releaseImplementation|ReleaseImplementation))\s+"""

    // Pattern to match type-safe accessors without parentheses
    private val TYPE_SAFE_ACCESSOR_REGEX = Regex(
      """${CONFIGURATION_PATTERN}((?:projects|libs)\.[\w.]+)(?!\s*\()""",
      RegexOption.MULTILINE
    )

    /**
     * Preprocesses the given content to normalize type-safe accessor syntax.
     */
    fun preprocess(originalContent: String): PreprocessingResult {
      val styleMap = mutableMapOf<String, Boolean>()
      
      val transformedContent = TYPE_SAFE_ACCESSOR_REGEX.replace(originalContent) { matchResult ->
        val configuration = matchResult.groupValues[1]
        val accessor = matchResult.groupValues[2]
        
        // Record that this accessor originally had no parentheses
        styleMap["$configuration $accessor"] = false
        
        // Transform to parentheses syntax
        "$configuration($accessor)"
      }
      
      return PreprocessingResult(
        content = transformedContent,
        originalContent = originalContent,
        styleMap = styleMap
      )
    }

    /**
     * Creates an InputStream from preprocessed content for the parser.
     */
    fun createInputStream(result: PreprocessingResult): InputStream {
      return result.content.byteInputStream()
    }

    /**
     * Checks if the given text represents a type-safe accessor that was preprocessed.
     */
    fun isPreprocessedAccessor(text: String): Boolean {
      return TypeSafeAccessorUtils.ACCESSOR_WITH_PARENS.find(text.trim()) != null
    }

    /**
     * Restores original syntax based on style information.
     */
    fun restoreOriginalSyntax(text: String, styleMap: Map<String, Boolean>): String {
      val match = TypeSafeAccessorUtils.ACCESSOR_WITH_PARENS.find(text.trim()) ?: return text
      
      val configuration = match.groupValues[1]
      val accessor = match.groupValues[2]
      val key = "$configuration $accessor"
      
      // Check if this was originally without parentheses
      val hadParentheses = styleMap[key] ?: true
      
      return if (hadParentheses) {
        text // Keep parentheses
      } else {
        "$configuration $accessor" // Remove parentheses
      }
    }
  }
}
