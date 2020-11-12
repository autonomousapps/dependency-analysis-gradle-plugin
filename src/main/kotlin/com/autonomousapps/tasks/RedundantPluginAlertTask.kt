package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Runs if both java-library and kotlin-jvm plugins have been applied. Checks for presence of java
 * and kotlin source. Suggests removing one or the other plugin as redundant, based on which kinds
 * of source are present.
 */
@CacheableTask
abstract class RedundantPluginAlertTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Produces a report about redundant jvm plugins that have been applied"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFiles
  abstract val javaFiles: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFiles
  abstract val kotlinFiles: ConfigurableFileCollection

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()

    val hasJava = javaFiles.files.isNotEmpty()
    val hasKotlin = kotlinFiles.files.isNotEmpty()

    val pluginAdvice = if (hasJava && hasKotlin) {
      // Project has both java and kotlin, prefer kotlin
      PluginAdvice.redundantJavaLibrary()
    } else if (hasJava) {
      // Project has only java, so why was kotlin applied?
      PluginAdvice.redundantKotlinJvm()
    } else {
      // Project has only kotlin, or nothing (!), prefer kotlin
      PluginAdvice.redundantJavaLibrary()
    }

    val pluginAdvices = listOf(pluginAdvice)

    if (pluginAdvices.isNotEmpty()) {
      val adviceString = pluginAdvices.joinToString(prefix = "- ", separator = "\n- ") {
        "${it.redundantPlugin}, because ${it.reason}"
      }
      logger.debug("Redundant plugins that should be removed:\n$adviceString")
    }

    outputFile.writeText(pluginAdvices.toJson())
  }
}
