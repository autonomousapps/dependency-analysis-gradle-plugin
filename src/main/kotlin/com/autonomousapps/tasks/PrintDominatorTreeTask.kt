package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.readText
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = "Always prints to console")
abstract class PrintDominatorTreeTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Prints a dominator view of the dependency graph"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val consoleText: RegularFileProperty

  @TaskAction fun action() {
    logger.quiet(consoleText.readText())
  }
}
