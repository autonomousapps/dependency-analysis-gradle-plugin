@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.ConsoleReport
import com.autonomousapps.internal.advice.AdvicePrinter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.shouldFail
import com.autonomousapps.shouldNotBeSilent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.*

abstract class ProjectHealth : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Consumes output of aggregateAdvice and can fail the build if desired"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val comprehensiveAdvice: RegularFileProperty

  @get:Input
  abstract val dependencyRenamingMap: MapProperty<String, String>

  @TaskAction fun action() {
    val inputFile = comprehensiveAdvice.get().asFile
    val comprehensiveAdvice = comprehensiveAdvice.fromJson<ComprehensiveAdvice>()

    val consoleReport = ConsoleReport.from(comprehensiveAdvice)
    val advicePrinter = AdvicePrinter(consoleReport, dependencyRenamingMap.orNull)
    val shouldFail = comprehensiveAdvice.shouldFail || shouldFail()
    val consoleText = advicePrinter.consoleText()

    // Only print to console if we're not configured to fail
    if (!shouldFail) {
      if (shouldNotBeSilent()) {
        logger.quiet(consoleText)
        if (consoleReport.isNotEmpty()) {
          logger.quiet("See machine-readable report at ${inputFile.path}")
        }
      } else {
        logger.debug(consoleText)
        if (consoleReport.isNotEmpty()) {
          logger.debug("See machine-readable report at ${inputFile.path}")
        }
      }
    }

    if (shouldFail) {
      throw GradleException(consoleText)
    }
  }
}
