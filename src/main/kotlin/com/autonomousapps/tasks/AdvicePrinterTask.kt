@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.*
import com.autonomousapps.internal.utils.*
import com.autonomousapps.internal.utils.chatter
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.lang.StringBuilder

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

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  abstract val adviceConsoleReport: RegularFileProperty

  @get:Input
  abstract val chatty: Property<Boolean>

  @get:OutputFile
  abstract val adviceConsoleReportTxt: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

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
      val advicePrinter = AdvicePrinter(consoleReport)
      var didGiveAdvice = false

      advicePrinter.getRemoveAdvice()?.let {
        consoleReportText.append("Unused dependencies which should be removed:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getAddAdvice()?.let {
        consoleReportText.append("Transitively used dependencies that should " +
          "be declared directly as indicated:\n$it\n\n")
        didGiveAdvice = true
      }

      advicePrinter.getChangeAdvice()?.let {
        consoleReportText.append("Existing dependencies which should be modified " +
          "to be as indicated:\n$it\n\n")
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
      chatter.chat(reportText)
      adviceConsoleReportTxtFile.writeText(reportText)
    }
  }
}
