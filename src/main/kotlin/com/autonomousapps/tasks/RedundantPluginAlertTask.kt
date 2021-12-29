package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Runs if both java-library and kotlin-jvm plugins have been applied. Checks for presence of java
 * and kotlin source. Suggests removing kotlin-jvm if there is no Kotlin source.
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

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()

    val hasKotlin = kotlinFiles.files.isNotEmpty()
    val shouldIgnore = redundantPluginsBehavior.get() is Ignore

    // TODO Issue 427: use plugin excludes
    // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/427
    val pluginAdvices =
      if (!hasKotlin && !shouldIgnore) setOf(PluginAdvice.redundantKotlinJvm())
      else emptySet()

    outputFile.writeText(pluginAdvices.toJson())
  }
}
