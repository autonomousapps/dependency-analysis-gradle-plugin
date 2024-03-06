// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildHealthException
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ProjectHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Prints advice for this project"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val consoleReport: RegularFileProperty

  @TaskAction fun action() {
    val consoleReport = consoleReport.get().asFile.readText()
    val projectAdvice = projectAdvice.fromJson<ProjectAdvice>()

    if (projectAdvice.shouldFail) {
      check(consoleReport.isNotBlank()) { "Console report should not be blank if projectHealth should fail" }
      throw BuildHealthException(consoleReport)
    } else if (consoleReport.isNotBlank()) {
      logger.quiet(consoleReport)
    }
  }
}
