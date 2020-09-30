@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.shouldNotBeSilent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

abstract class BuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates holistic advice for whole project, and can fail the build if desired"
  }

  /**
   * A `List<BuildHealth>`.
   */
  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val adviceReport: RegularFileProperty

  @get:Input
  abstract val dependencyRenamingMap: MapProperty<String, String>

  @TaskAction fun action() {
    val buildHealth = adviceReport.fromJsonList<ComprehensiveAdvice>()

    val mapper = dependencyRenamingMap.orNull
    val inputFilePath = adviceReport.get().asFile.path

    buildHealth.forEach { projectAdvice ->
      val consoleReport = ConsoleReport.from(projectAdvice)
      val advicePrinter = AdvicePrinter(consoleReport, mapper)

      val consoleText = advicePrinter.consoleText()
      if (shouldNotBeSilent()) {
        logger.quiet(projectHeaderText(projectAdvice.projectPath))
        logger.quiet(consoleText)
        if (consoleReport.isNotEmpty()) {
          logger.quiet("See machine-readable report at $inputFilePath\n")
        }
      } else {
        logger.quiet(projectHeaderText(projectAdvice.projectPath))
        logger.debug(consoleText)
        if (consoleReport.isNotEmpty()) {
          logger.debug("See machine-readable report at $inputFilePath\n")
        }
      }
    }

    if (buildHealth.any { it.shouldFail }) {
      throw GradleException("Dependency Analysis Gradle Plugin has detected fatal issues. Please see advice reports")
    }
  }

  private fun projectHeaderText(projectPath: String): String =
    if (projectPath == ":") "Advice for root project"
    else "Advice for project $projectPath"
}
