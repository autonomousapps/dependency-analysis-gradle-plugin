package com.autonomousapps.convention.tasks.metalava

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
public abstract class UpdateApiTask : DefaultTask() {

  init {
    group = MetalavaConfigurer.TASK_GROUP
    description = "Updates the api file at api/api.txt."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFile
  public abstract val input: RegularFileProperty

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    input.get().asFile.copyTo(output.get().asFile, overwrite = true)
  }
}
