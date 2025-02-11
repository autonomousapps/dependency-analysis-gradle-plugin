// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.exception.BuildHealthException
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ProjectHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Prints advice for this project"
  }

  @get:Input
  abstract val buildFilePath: Property<String>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val consoleReport: RegularFileProperty

  @TaskAction fun action() {
    val consoleReport = consoleReport.get().asFile.readText()
    val projectAdvice = projectAdvice.fromJson<ProjectAdvice>()

    val hasText = consoleReport.isNotBlank()

    if (projectAdvice.shouldFail) {
      check(hasText) { "Console report should not be blank if projectHealth should fail" }
      throw BuildHealthException(prependBuildPath(consoleReport))
    } else if (hasText) {
      logger.quiet(prependBuildPath(consoleReport))
    }
  }

  private fun prependBuildPath(consoleReport: String): String {
    return "${buildFilePath.get()}\n$consoleReport"
  }
}
