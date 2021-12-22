package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildHealthException
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class BuildHealthTask2 : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates holistic advice for whole project, and can fail the build if desired"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val consoleReport: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val buildHealth: RegularFileProperty

  @TaskAction fun action() {
    val consoleReportFile = consoleReport.get().asFile
    val consoleReportText = consoleReport.get().asFile.readText()
    val consoleReportPath = consoleReportFile.absolutePath
    val buildHealth = buildHealth.fromJsonSet<ProjectAdvice>()

    val output = "See report at $consoleReportPath"

    if (buildHealth.any { it.shouldFail }) {
      check(consoleReportText.isNotBlank()) { "Console report should not be blank if buildHealth should fail" }
      throw BuildHealthException(output)
    } else if (consoleReportText.isNotBlank()) {
      logger.quiet(output)
    }
  }
}
