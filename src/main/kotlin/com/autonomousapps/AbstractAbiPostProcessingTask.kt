package com.autonomousapps

import com.autonomousapps.internal.utils.readText
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

abstract class AbstractAbiPostProcessingTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val input: RegularFileProperty

  fun abiDump(): String {
    return input.readText()
  }
}
