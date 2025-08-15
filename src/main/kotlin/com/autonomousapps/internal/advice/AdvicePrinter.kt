// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectCoordinates

internal class AdvicePrinter(
  private val dslKind: DslKind,
  /** Customize how dependencies are printed. */
  private val dependencyMap: ((String) -> String?)? = null,
  private val useTypesafeProjectAccessors: Boolean,
  /** Whether to use parentheses syntax for Kotlin DSL. If false, uses space syntax like Groovy. */
  private val useParenthesesSyntax: Boolean = true,
) {

  val usesTypesafeProjectAccessors: Boolean get() = useTypesafeProjectAccessors
  val getDependencyMap: ((String) -> String?)? get() = dependencyMap

  private companion object {
    val PROJECT_PATH_PATTERN = "[-_][a-z0-9]".toRegex()

    fun String.kebabOrSnakeToCamelCase(): String {
      return replace(PROJECT_PATH_PATTERN) {
        it.value.last().uppercaseChar().toString()
      }
    }
  }

  fun line(configuration: String, printableIdentifier: String, was: String = ""): String =
    "  $configuration$printableIdentifier$was"

  fun toDeclaration(advice: Advice): String =
    "  ${advice.toConfiguration}${gav(advice.coordinates)}"

  fun gav(coordinates: Coordinates): String {
    val quotedDep = coordinates.mapped()

    return when (coordinates) {
      is ProjectCoordinates -> getProjectFormat(quotedDep)
      else -> quotedDep
    }.let { id ->
      if (coordinates.gradleVariantIdentification.capabilities.isEmpty()) {
        when (dslKind) {
          DslKind.KOTLIN -> if (useParenthesesSyntax) "($id)" else " $id"
          DslKind.GROOVY -> " $id"
        }
      } else if (coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(GradleVariantIdentification.TEST_FIXTURES) }) {
        when (dslKind) {
          DslKind.KOTLIN -> "(testFixtures($id))"
          DslKind.GROOVY -> " testFixtures($id)"
        }
      } else {
        val quote = when (dslKind) {
          DslKind.KOTLIN -> "\""
          DslKind.GROOVY -> "'"
        }
        "($id) { capabilities {\n${
          coordinates.gradleVariantIdentification.capabilities
            .filter { !it.endsWith(":test-fixtures") }
            .joinToString("") { it.requireCapability(quote) }
        }  }}"
      }
    }
  }

  private fun getProjectFormat(quotedDep: String): String {
    return if (useTypesafeProjectAccessors) {
      if (dslKind == DslKind.KOTLIN) {
        "projects${quotedDep.replace(':', '.').replace("\"", "").kebabOrSnakeToCamelCase()}"
      } else {
        "projects${quotedDep.replace(':', '.').replace("'", "").kebabOrSnakeToCamelCase()}"
      }
    } else {
      "project(${quotedDep})"
    }
  }

  private fun String.requireCapability(quote: String) = "    requireCapability($quote$this$quote)\n"

  private fun Coordinates.mapped(): String {
    val gav = gav()
    // if the map contains full GAV
    val mapped = dependencyMap?.invoke(gav) ?: dependencyMap?.invoke(identifier)

    return if (!mapped.isNullOrBlank()) {
      // If the user is mapping, it's bring-your-own-quotes
      mapped
    } else {
      // If there's no map, include quotes
      when (dslKind) {
        DslKind.KOTLIN -> "\"$gav\""
        DslKind.GROOVY -> "'$gav'"
      }
    }
  }
}
