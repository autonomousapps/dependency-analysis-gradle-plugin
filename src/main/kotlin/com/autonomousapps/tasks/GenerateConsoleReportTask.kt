@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class GenerateConsoleReportTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates console report"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ProjectHealthAction::class.java) {
      advice.set(this@GenerateConsoleReportTask.projectAdvice)
      output.set(this@GenerateConsoleReportTask.output)
    }
  }
}

interface ProjectHealthParameters : WorkParameters {
  val advice: RegularFileProperty
  val output: RegularFileProperty
}

abstract class ProjectHealthAction : WorkAction<ProjectHealthParameters> {

  override fun execute() {
    val output = parameters.output.getAndDelete()

    val projectAdvice = parameters.advice.fromJson<ProjectAdvice>()
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

    val consoleText = buildString {
      var shouldPrintNewLine = false

      if (removeAdvice.isNotEmpty()) {
        shouldPrintNewLine = true

        appendReproducibleNewLine("Unused dependencies which should be removed:")
        val toPrint = removeAdvice.joinToString(separator = "\n") {
          " ${it.fromConfiguration}(${printableIdentifier(it.coordinates)})"
        }
        append(toPrint)
      }

      if (addAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Transitively used dependencies that should be declared directly as indicated:")
        val toPrint = addAdvice.joinToString(separator = "\n") {
          " ${it.toConfiguration}(${printableIdentifier(it.coordinates)})"
        }
        append(toPrint)
      }

      if (changeAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Existing dependencies which should be modified to be as indicated:")
        val toPrint = changeAdvice.joinToString(separator = "\n") {
          " ${it.toConfiguration}(${printableIdentifier(it.coordinates)}) (was ${it.fromConfiguration})"
        }
        append(toPrint)
      }

      if (compileOnlyAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Dependencies which could be compile-only:")
        val toPrint = compileOnlyAdvice.joinToString(separator = "\n") {
          " ${it.toConfiguration}(${printableIdentifier(it.coordinates)}) (was ${it.fromConfiguration})"
        }
        append(toPrint)
      }

      if (processorAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }
        shouldPrintNewLine = true

        appendReproducibleNewLine("Unused annotation processors that should be removed:")
        val toPrint = processorAdvice.joinToString(separator = "\n") {
          " ${it.fromConfiguration}(${printableIdentifier(it.coordinates)})"
        }
        append(toPrint)
      }

      val pluginAdvice = projectAdvice.pluginAdvice
      if (pluginAdvice.isNotEmpty()) {
        if (shouldPrintNewLine) {
          appendReproducibleNewLine()
          appendReproducibleNewLine()
        }

        appendReproducibleNewLine("Unused plugins that can be removed:")
        val toPrint = pluginAdvice.joinToString(separator = "\n") {
          " ${it.redundantPlugin}: ${it.reason}"
        }
        append(toPrint)
      }
    }

    output.writeText(consoleText)
  }

  private fun printableIdentifier(coordinates: Coordinates): String {
    val gav = coordinates.gav()
    return when (coordinates) {
      is ProjectCoordinates -> "project(\"${gav}\")"
      else -> "\"$gav\""
    }
  }
}

// val inputFile = comprehensiveAdvice.get().asFile
//
// val consoleReport = ConsoleReport.from(compAdvice)
// val advicePrinter = AdvicePrinter(consoleReport, dependencyRenamingMap.orNull)
// val shouldFail = compAdvice.shouldFail || shouldFail()
// val consoleText = advicePrinter.consoleText()
//
// // Only print to console if we're not configured to fail
// if (!shouldFail && consoleReport.isNotEmpty()) {
//   logger.quiet(consoleText)
//   if (shouldNotBeSilent()) {
//     logger.quiet(metricsText)
//     logger.quiet("See machine-readable report at ${inputFile.path}")
//   }
// }
//
// if (shouldFail) {
//   throw BuildHealthException(consoleText)
// }
