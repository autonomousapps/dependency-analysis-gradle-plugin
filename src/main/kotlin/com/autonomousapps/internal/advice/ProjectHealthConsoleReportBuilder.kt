package com.autonomousapps.internal.advice

import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

internal class ProjectHealthConsoleReportBuilder(
  private val projectAdvice: ProjectAdvice
) {

  val text: String

  init {
    val dependencyAdvice = projectAdvice.dependencyAdvice
    val removeAdvice = mutableSetOf<Advice>()
    val addAdvice = mutableSetOf<Advice>()
    val changeAdvice = mutableSetOf<Advice>()
    val compileOnlyAdvice = mutableSetOf<Advice>()
    val processorAdvice = mutableSetOf<Advice>()

    dependencyAdvice.forEach { advice ->
      if (advice.isRemove()) removeAdvice += advice
      if (advice.isAdd()) addAdvice += advice
      if (advice.isChange()) changeAdvice += advice
      if (advice.isCompileOnly()) compileOnlyAdvice += advice
      if (advice.isProcessor()) processorAdvice += advice
    }

    text = buildString {
      var shouldPrintNewLine = false

      if (removeAdvice.isNotEmpty()) {
        shouldPrintNewLine = true

        appendReproducibleNewLine("Unused dependencies which should be removed:")
        val toPrint = removeAdvice.mapToOrderedSet {
          "  ${it.fromConfiguration}(${printableIdentifier(it.coordinates)})"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      if (addAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Transitively used dependencies that should be declared directly as indicated:")
        val toPrint = addAdvice.mapToOrderedSet {
          "  ${it.toConfiguration}(${printableIdentifier(it.coordinates)})"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      if (changeAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Existing dependencies which should be modified to be as indicated:")
        val toPrint = changeAdvice.mapToOrderedSet {
          "  ${it.toConfiguration}(${printableIdentifier(it.coordinates)}) (was ${it.fromConfiguration})"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      if (compileOnlyAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Dependencies which could be compile-only:")
        val toPrint = compileOnlyAdvice.mapToOrderedSet {
          "  ${it.toConfiguration}(${printableIdentifier(it.coordinates)}) (was ${it.fromConfiguration})"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      if (processorAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Unused annotation processors that should be removed:")
        val toPrint = processorAdvice.mapToOrderedSet {
          "  ${it.fromConfiguration}(${printableIdentifier(it.coordinates)})"
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      val pluginAdvice = projectAdvice.pluginAdvice
      if (pluginAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }

        appendReproducibleNewLine("Unused plugins that can be removed:")
        val toPrint = pluginAdvice.mapToOrderedSet {
          "  ${it.redundantPlugin}: ${it.reason}"
        }.joinToString(separator = "\n")
        append(toPrint)
      }
    }
  }

  private fun printableIdentifier(coordinates: Coordinates): String {
    val gav = coordinates.gav()
    return when (coordinates) {
      is ProjectCoordinates -> "project(\"${gav}\")"
      else -> "\"$gav\""
    }
  }
}
