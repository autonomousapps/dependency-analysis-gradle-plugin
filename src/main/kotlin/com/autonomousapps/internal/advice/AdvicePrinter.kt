package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates

internal class AdvicePrinter(
  private val dslKind: DslKind,
  /** Customize how dependencies are printed. */
  private val dependencyMap: (String) -> String = { it },
) {

  fun line(configuration: String, printableIdentifier: String, was: String = ""): String =
    "  $configuration$printableIdentifier$was"

  fun toDeclaration(advice: Advice): String =
    "  ${advice.toConfiguration}${gav(advice.coordinates)}"

  fun gav(coordinates: Coordinates): String {
    val quotedDep = coordinates.mapped()
    return when (coordinates) {
      is ProjectCoordinates -> if (dslKind == DslKind.KOTLIN) "project(${quotedDep})" else "project(${quotedDep})"
      else -> if (dslKind == DslKind.KOTLIN) quotedDep else quotedDep
    }.let { id ->
      if (coordinates.gradleVariantIdentification.capabilities.isEmpty()) {
        when (dslKind) {
          DslKind.KOTLIN -> "($id)"
          DslKind.GROOVY -> " $id"
        }
      } else if (coordinates.gradleVariantIdentification.capabilities.any { it.endsWith("-test-fixtures") }) {
        when (dslKind) {
          DslKind.KOTLIN -> "(testFixtures($id))"
          DslKind.GROOVY -> " testFixtures($id)"
        }
      } else {
        val quote = when (dslKind) {
          DslKind.KOTLIN -> "\""
          DslKind.GROOVY -> "'"
        }
        "($id) { capabilities {\n${coordinates.gradleVariantIdentification.capabilities.filter { !it.endsWith(":test-fixtures") }.joinToString("") { it.requireCapability(quote) }}  }}"
      }
    }
  }

  private fun String.requireCapability(quote: String) = "    requireCapability($quote$this$quote)\n"

  private fun Coordinates.mapped(): String {
    val gav = gav()
    val mapped = dependencyMap(gav)

    return if (gav == mapped) {
      // If there's no map, include quotes
      when (dslKind) {
        DslKind.KOTLIN -> "\"$mapped\""
        DslKind.GROOVY -> "'$mapped'"
      }
    } else {
      // If the user is mapping, it's bring-your-own-quotes
      mapped
    }
  }
}
