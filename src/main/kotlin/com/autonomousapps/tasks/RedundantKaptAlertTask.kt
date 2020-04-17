package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.AnnotationProcessor
import com.autonomousapps.internal.utils.chatter
import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

/**
 * Takes as input (1) declaredProcs and (2) unused procs. Runs only if kotlin-kapt has been applied.
 * If it determines there are either (1) no procs declared or (2) no _used_ procs declared, suggests
 * removing kapt.
 */
@CacheableTask
abstract class RedundantKaptAlertTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report indicating if kapt is redundant"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declaredProcs: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unusedProcs: RegularFileProperty

  @get:Input
  abstract val chatty: Property<Boolean>

  @get:OutputFile
  abstract val output: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val declaredProcs = declaredProcs.fromJsonSet<AnnotationProcessor>()
    val unusedProcs = unusedProcs.fromJsonSet<AnnotationProcessor>()

    val pluginAdvice =
      if (declaredProcs.isEmpty() || (declaredProcs - unusedProcs).isEmpty()) {
        // Kapt is unused
        setOf(PluginAdvice.redundantKapt())
      } else {
        emptySet()
      }

    if (pluginAdvice.isNotEmpty()) {
      val adviceString = pluginAdvice.joinToString(prefix = "- ", separator = "\n- ") {
        "${it.redundantPlugin}, because ${it.reason}"
      }
      chatter.chat("Redundant plugins that should be removed:\n$adviceString")
    }

    outputFile.writeText(pluginAdvice.toJson())
  }
}
