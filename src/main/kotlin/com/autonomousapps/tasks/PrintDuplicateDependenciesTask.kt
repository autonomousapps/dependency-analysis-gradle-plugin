// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.readText
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@UntrackedTask(because = "Always prints output")
public abstract class PrintDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Prints report of dependencies that have multiple versions across the build."
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val duplicateDependenciesReport: RegularFileProperty

  @TaskAction public fun action() {
    val consoleReport = duplicateDependenciesReport.readText()
    logger.quiet(consoleReport)
  }
}
