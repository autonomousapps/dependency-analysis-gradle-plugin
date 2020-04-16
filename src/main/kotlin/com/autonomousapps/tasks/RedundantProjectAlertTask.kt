package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class RedundantProjectAlertTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report about redundant jvm plugins that have been applied"
  }

  // TODO add an input for kotlin and java source, so we can check which language(s) are present.

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.get().asFile
    outputFile.delete()

    val pluginAdvice = listOf(PluginAdvice.redundantPlugin())

    outputFile.writeText(pluginAdvice.toJson())
  }
}
