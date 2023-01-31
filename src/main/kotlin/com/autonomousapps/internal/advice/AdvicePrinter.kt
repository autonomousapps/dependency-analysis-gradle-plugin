package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
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
    }.let {
      if (coordinates.featureVariantName.isEmpty()) {
        when (dslKind) {
          DslKind.KOTLIN -> "($it)"
          DslKind.GROOVY -> " $it"
        }
      } else if (coordinates.featureVariantName == "test-fixtures") {
        when (dslKind) {
          DslKind.KOTLIN -> "(testFixtures($it))"
          DslKind.GROOVY -> " testFixtures($it)"
        }
      } else {
        val capability = when (coordinates) {
          // TODO could probably pass the group around in ProjectCoordinates
          is ProjectCoordinates -> "[group]${coordinates.identifier}-${coordinates.featureVariantName}"
          is ModuleCoordinates -> "${coordinates.identifier}-${coordinates.featureVariantName}"
          else -> "[group]:${coordinates.identifier}-${coordinates.featureVariantName}"
        }
        val quote = when (dslKind) {
          DslKind.KOTLIN -> "\""
          DslKind.GROOVY -> "'"
        }
        "($it) { capabilities { requireCapabilities($quote$capability$quote) } }"
      }
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
