package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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

  @get:Input
  abstract val hasJava: Property<Boolean>

  @get:Input
  abstract val hasKotlin: Property<Boolean>

  @get:Input
  abstract val redundantPluginsBehavior: Property<Behavior>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    // Outputs
    val outputFile = output.getAndDelete()

    val shouldIgnore = redundantPluginsBehavior.get() is Ignore

    // TODO Issue 427: use plugin excludes
    // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/427
    val pluginAdvices =
      if (!hasKotlin.get() && !shouldIgnore) setOf(PluginAdvice.redundantKotlinJvm())
      else emptySet()

    outputFile.writeText(pluginAdvices.toJson())
  }
}
