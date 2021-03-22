package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class FinalAdviceTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Saves final advice to file (strict or minimized)"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val buildHealth: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    buildHealth.get().asFile.copyTo(outputFile, overwrite = true)
  }
}
