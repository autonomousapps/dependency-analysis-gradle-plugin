// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.ProjectType
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectCoordinates

internal class AdvicePrinter(
  private val dslKind: DslKind,
  private val projectType: ProjectType,
  /** Customize how dependencies are printed. */
  private val dependencyMap: ((String) -> String?)? = null,
  private val useTypesafeProjectAccessors: Boolean,
  private val useParenthesesForGroovy: Boolean = false,
) {

  private companion object {
    val PROJECT_PATH_PATTERN = "[-_][a-z0-9]".toRegex()

    fun String.kebabOrSnakeToCamelCase(): String {
      return replace(PROJECT_PATH_PATTERN) {
        it.value.last().uppercaseChar().toString()
      }
    }
  }

  fun line(configuration: String, printableIdentifier: String, was: String = ""): String {
    return "  $configuration$printableIdentifier$was"
  }

  fun toDeclaration(advice: Advice): String {
    return "  ${advice.toConfiguration}${gav(advice.coordinates)}"
  }

  fun gav(coordinates: Coordinates): String {
    val quotedDep = coordinates.mapped()

    return when (coordinates) {
      is ProjectCoordinates -> {
        val projectFormat = getProjectFormat(quotedDep)
        when (dslKind) {
          DslKind.KOTLIN -> "($projectFormat)"
          DslKind.GROOVY -> if (useParenthesesForGroovy) "($projectFormat)" else " $projectFormat"
        }
      }

      else -> quotedDep
    }.let { id ->
      if (coordinates.gradleVariantIdentification.capabilities.any { it.endsWith(GradleVariantIdentification.TEST_FIXTURES) }) {
        val cleanId = if (coordinates is ProjectCoordinates && id.startsWith("(") && id.endsWith(")")) {
          id.substring(1, id.length - 1) // Remove outer parentheses
        } else {
          id
        }
        when (dslKind) {
          DslKind.KOTLIN -> "(testFixtures($cleanId))"
          DslKind.GROOVY -> " testFixtures($cleanId)"
        }
      } else if (coordinates is ProjectCoordinates) {
        // ProjectCoordinates are already handled above, just return the formatted ID
        id
      } else if (coordinates.gradleVariantIdentification.capabilities.isEmpty()) {
        when (dslKind) {
          DslKind.KOTLIN -> "($id)"
          DslKind.GROOVY -> if (useParenthesesForGroovy) "($id)" else " $id"
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
