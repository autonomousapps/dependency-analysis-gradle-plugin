// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildHealthException
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
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

  @get:Input
  abstract val printBuildHealth: Property<Boolean>

  @TaskAction fun action() {
    val shouldFail = shouldFail.get().asFile.readText().toBoolean()
    val consoleReportFile = consoleReport.get().asFile
    val consoleReportPath = consoleReportFile.toPath()
    val hasAdvice = consoleReportFile.length() > 0

    val output = buildString {
      if (printBuildHealth.get()) {
        append(consoleReportFile.readText())
      }
      // Trailing space so terminal UIs linkify it
      append("There were dependency violations. See report at ${consoleReportPath.toUri()} ")
    }

    if (shouldFail) {
      check(hasAdvice) { "Console report should not be blank if buildHealth should fail" }
      throw BuildHealthException(output)
    } else if (hasAdvice) {
      logger.quiet(output)
    }
  }
}
