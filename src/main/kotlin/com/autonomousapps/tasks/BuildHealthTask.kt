@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.exception.BuildHealthException
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.ProjectMetrics
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.getMetricsText
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.fromJsonMap
import com.autonomousapps.shouldFail
import com.autonomousapps.shouldNotBeSilent
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

abstract class BuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates holistic advice for whole project, and can fail the build if desired"
  }

  /**
   * A `List<ComprehensiveAdvice>`.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val adviceReport: RegularFileProperty

  @get:Input
  abstract val dependencyRenamingMap: MapProperty<String, String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val buildMetricsJson: RegularFileProperty

  private val buildMetrics by lazy {
    buildMetricsJson.fromJsonMap<String, ProjectMetrics>()
  }

  @TaskAction fun action() {
    val buildHealth = adviceReport.fromJsonList<ComprehensiveAdvice>()

    val mapper = dependencyRenamingMap.orNull
    val inputFilePath = adviceReport.get().asFile.path

    var shouldPrintPath = false
    val buildHealthText = StringBuilder()
    val shouldFail = buildHealth.any { it.shouldFail } || shouldFail()

    buildHealth.forEach { projectAdvice ->
      val consoleReport = ConsoleReport.from(projectAdvice)
      val advicePrinter = AdvicePrinter(consoleReport, mapper)

      if (consoleReport.isNotEmpty()) shouldPrintPath = true

      val consoleText = advicePrinter.consoleText()
      if (consoleText.isNotEmpty()) {
        // The advice is indented
        val msg = projectHeaderText(projectAdvice.projectPath) + "\n  " +
          consoleText.replace("\n", "\n  ")
        buildHealthText.appendReproducibleNewLine(msg)

        // Only print to console if we're not configured to fail
        if (!shouldFail) {
          // Print the advice with a single logging call so they do not shuffle with other log lines
          // from the other tasks
          logger.quiet(msg)
        }
      }
    }
    if (shouldPrintPath) {
      if (shouldNotBeSilent()) {
        logger.quiet(metricsText)
        logger.quiet("See machine-readable report at $inputFilePath\n")
      } else {
        logger.debug(metricsText)
        logger.debug("See machine-readable report at $inputFilePath\n")
      }
    }

    if (shouldFail) {
      throw BuildHealthException(buildHealthText.toString())
    }
  }

  private fun projectHeaderText(projectPath: String): String =
    if (projectPath == ":") "Advice for root project"
    else "Advice for project $projectPath"

  private val metricsText by lazy {
    getMetricsText(buildMetrics)
  }
}
