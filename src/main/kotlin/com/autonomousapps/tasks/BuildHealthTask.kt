package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildHealthException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class BuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates holistic advice for whole project, and can fail the build if desired"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val shouldFail: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val consoleReport: RegularFileProperty

  @TaskAction fun action() {
    val shouldFail = shouldFail.get().asFile.readText().toBoolean()
    val consoleReportFile = consoleReport.get().asFile
    val consoleReportPath = consoleReportFile.absolutePath
    val hasAdvice = consoleReportFile.length() > 0

    val output = "There were dependency violations. See report at $consoleReportPath"

    if (shouldFail) {
      check(hasAdvice) { "Console report should not be blank if buildHealth should fail" }
      throw BuildHealthException(output)
    } else if (hasAdvice) {
      logger.quiet(output)
    }
  }
}
