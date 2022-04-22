package com.autonomousapps.internal.advice

import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

internal class ProjectHealthConsoleReportBuilder(
  private val projectAdvice: ProjectAdvice,
  private val dslKind: DslKind
) {

  val text: String

  init {
    val dependencyAdvice = projectAdvice.dependencyAdvice
    val removeAdvice = mutableSetOf<Advice>()
    val addAdvice = mutableSetOf<Advice>()
    val changeAdvice = mutableSetOf<Advice>()
    val runtimeOnlyAdvice = mutableSetOf<Advice>()
    val compileOnlyAdvice = mutableSetOf<Advice>()
    val processorAdvice = mutableSetOf<Advice>()

    dependencyAdvice.forEach { advice ->
      if (advice.isRemove()) removeAdvice += advice
      if (advice.isAdd()) addAdvice += advice
      if (advice.isChange()) changeAdvice += advice
      if (advice.isRuntimeOnly()) runtimeOnlyAdvice += advice
      if (advice.isCompileOnly()) compileOnlyAdvice += advice
      if (advice.isProcessor()) processorAdvice += advice
    }

    text = buildString {
      var shouldPrintNewLine = false

      if (removeAdvice.isNotEmpty()) {
        shouldPrintNewLine = true

        appendReproducibleNewLine("Unused dependencies which should be removed:")
        val toPrint = removeAdvice.mapToOrderedSet {
          line(it.fromConfiguration!!, printableIdentifier(it.coordinates))
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
          line(it.toConfiguration!!, printableIdentifier(it.coordinates))
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
          line(it.toConfiguration!!, printableIdentifier(it.coordinates), " (was ${it.fromConfiguration})")
        }.joinToString(separator = "\n")
        append(toPrint)
      }

      if (runtimeOnlyAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Dependencies which should be removed or changed to runtime-only:")
        val toPrint = runtimeOnlyAdvice.mapToOrderedSet {
          line(it.toConfiguration!!, printableIdentifier(it.coordinates), " (was ${it.fromConfiguration})")
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
          line(it.toConfiguration!!, printableIdentifier(it.coordinates), " (was ${it.fromConfiguration})")
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
          line(it.fromConfiguration!!, printableIdentifier(it.coordinates))
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

  private fun line(configuration: String, printableIdentifier: String, was: String = ""): String = when (dslKind) {
    DslKind.KOTLIN -> "  $configuration($printableIdentifier)$was"
    DslKind.GROOVY -> "  $configuration $printableIdentifier$was"
  }

  private fun printableIdentifier(coordinates: Coordinates): String {
    val gav = coordinates.gav()
    return when (coordinates) {
      is ProjectCoordinates -> if (dslKind == DslKind.KOTLIN) "project(\"${gav}\")" else "project('${gav}')"
      else -> if (dslKind == DslKind.KOTLIN) "\"$gav\"" else "'$gav'"
    }
  }
}
