@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

/**
 * Produces human-readable advice files and console report on how to modify a project's
 * dependencies in order to have a healthy build.
 */
@CacheableTask
abstract class AdvicePrinterTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
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

    if (consoleReport.isEmpty()) {
      consoleReportText.append("Looking good! No changes needed")
    } else {
      val advicePrinter = AdvicePrinter(consoleReport, dependencyRenamingMap.orNull)
      var didGiveAdvice = false

      advicePrinter.getRemoveAdvice()?.let {
        consoleReportText.append("Unused dependencies which should be removed:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getAddAdvice()?.let {
        consoleReportText.append("Transitively used dependencies that should be declared directly as indicated:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getChangeAdvice()?.let {
        consoleReportText.append("Existing dependencies which should be modified to be as indicated:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getCompileOnlyAdvice()?.let {
        consoleReportText.append("Dependencies which could be compile-only:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getRemoveProcAdvice()?.let {
        consoleReportText.append("Unused annotation processors that should be removed:\n$it\n\n")
        didGiveAdvice = true
      }

      if (didGiveAdvice) {
        consoleReportText.append("See console report at ${adviceConsoleReportTxtFile.path}")
      } else {
        consoleReportText.append("Looking good! No changes needed")
      }

      val reportText = consoleReportText.toString()
      logger.debug(reportText)
      adviceConsoleReportTxtFile.writeText(reportText)
    }
  }
}
