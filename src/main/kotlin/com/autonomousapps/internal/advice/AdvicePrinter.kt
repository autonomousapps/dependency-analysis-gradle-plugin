package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates

internal class AdvicePrinter(
  private val dslKind: DslKind,
  /** Customize how dependencies are printed. */
  private val dependencyMap: (String) -> String = { it },
  private val useTypesafeProjectAccessors: Boolean
) {

  fun line(configuration: String, printableIdentifier: String, was: String = ""): String = when (dslKind) {
    DslKind.KOTLIN -> "  $configuration($printableIdentifier)$was"
    DslKind.GROOVY -> "  $configuration $printableIdentifier$was"
  }

  fun toDeclaration(advice: Advice): String = when (dslKind) {
    DslKind.KOTLIN -> "  ${advice.toConfiguration}(${gav(advice.coordinates)})"
    DslKind.GROOVY -> "  ${advice.toConfiguration} ${gav(advice.coordinates)}"
  }

  fun gav(coordinates: Coordinates): String {
    val quotedDep = coordinates.mapped()
    val dependency =  when (coordinates) {
      is ProjectCoordinates -> getProjectFormat(quotedDep)
      else -> if (dslKind == DslKind.KOTLIN) quotedDep else quotedDep
    }
    return dependency
  }

  private fun getProjectFormat(quotedDep: String): String {
    return if (useTypesafeProjectAccessors) {
      if (dslKind == DslKind.KOTLIN) {
        "projects${quotedDep.replace(':', '.').replace("'", "").kebabToCamelCase()}"
      } else {
        "projects${quotedDep.replace(':', '.').replace("'", "").kebabToCamelCase()}"
      }
    } else {
      if (dslKind == DslKind.KOTLIN) "project(${quotedDep})" else "project(${quotedDep})"
    }
  }

  private fun String.kebabToCamelCase(): String {
    val pattern = "-[a-z]".toRegex()
    return replace(pattern) {
      it.value.last().toUpperCase().toString()
    }
  }

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
