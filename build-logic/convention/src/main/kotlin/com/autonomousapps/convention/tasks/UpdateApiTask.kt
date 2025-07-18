package com.autonomousapps.convention.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

public abstract class UpdateApiTask : DefaultTask() {

  init {
    group = "TODO"
    description = "TODO"
  }

  @get:InputFile
  public abstract val input: RegularFileProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    input.get().asFile.copyTo(output.get().asFile, overwrite = true)
  }
}
