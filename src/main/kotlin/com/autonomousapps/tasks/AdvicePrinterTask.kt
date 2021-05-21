@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

/**
 * Produces human-readable advice files and console report on how to modify a project's
 * dependencies in order to have a healthy build.
 */
@CacheableTask
abstract class AdvicePrinterTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Displays advice on screen"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val adviceConsoleReport: RegularFileProperty

  @get:Input
  abstract val dependencyRenamingMap: MapProperty<String, String>

  @get:OutputFile
  abstract val adviceConsoleReportTxt: RegularFileProperty

  @TaskAction
  fun action() {
    // Output
    val adviceConsoleReportTxtFile = adviceConsoleReportTxt.getAndDelete()

    // Inputs
    val consoleReport = adviceConsoleReport.fromJson<ConsoleReport>()

    val consoleReportText = StringBuilder()

    if (consoleReport.isNotEmpty()) {
      val advicePrinter = AdvicePrinter(consoleReport, dependencyRenamingMap.orNull)
      var didGiveAdvice = false

      advicePrinter.getRemoveAdvice()?.let {
        consoleReportText.appendReproducibleNewLine(it)
        didGiveAdvice = true
      }

      advicePrinter.getAddAdvice()?.let {
        consoleReportText.appendReproducibleNewLine(it)
        didGiveAdvice = true
      }

      advicePrinter.getChangeAdvice()?.let {
        consoleReportText.appendReproducibleNewLine(it)
        didGiveAdvice = true
      }

      advicePrinter.getCompileOnlyAdvice()?.let {
        consoleReportText.appendReproducibleNewLine(it)
        didGiveAdvice = true
      }

      advicePrinter.getRemoveProcAdvice()?.let {
        consoleReportText.appendReproducibleNewLine(it)
        didGiveAdvice = true
      }

      if (didGiveAdvice) {
        consoleReportText.append("See console report at ${adviceConsoleReportTxtFile.path}")
      }

      val reportText = consoleReportText.toString()
      if (reportText.isNotBlank()) {
        logger.debug(reportText)
      }
      adviceConsoleReportTxtFile.writeText(reportText)
    }
  }
}
