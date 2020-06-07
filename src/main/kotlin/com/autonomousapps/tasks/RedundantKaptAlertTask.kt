@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.AnnotationProcessor
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

/**
 * Takes as input (1) declaredProcs and (2) unused procs. Runs only if kotlin-kapt has been applied.
 * If it determines there are either (1) no procs declared or (2) no _used_ procs declared, suggests
 * removing kapt.
 */
@CacheableTask
abstract class RedundantKaptAlertTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a report indicating if kapt is redundant"
  }

  @get:Input
  abstract val kapt: Property<Boolean>

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val declaredProcs: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val unusedProcs: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(RedundantKaptAlertWorkAction::class.java) {
      kapt.set(this@RedundantKaptAlertTask.kapt)
      declaredProcs.set(this@RedundantKaptAlertTask.declaredProcs)
      unusedProcs.set(this@RedundantKaptAlertTask.unusedProcs)
      output.set(this@RedundantKaptAlertTask.output)
    }
  }
}

interface RedundantKaptAlertParameters : WorkParameters {
  val kapt: Property<Boolean>
  val declaredProcs: RegularFileProperty
  val unusedProcs: RegularFileProperty
  val output: RegularFileProperty
}

abstract class RedundantKaptAlertWorkAction : WorkAction<RedundantKaptAlertParameters> {

  private val logger = getLogger<RedundantKaptAlertTask>()

  override fun execute() {
    val outputFile = parameters.output.getAndDelete()

    val pluginAdvice = if (!parameters.kapt.get()) {
      // kapt is not applied
      emptySet()
    } else {
      // kapt is applied
      val declaredProcs = parameters.declaredProcs.fromJsonSet<AnnotationProcessor>()
      val unusedProcs = parameters.unusedProcs.fromJsonSet<AnnotationProcessor>()

      if (declaredProcs.isEmpty() || (declaredProcs - unusedProcs).isEmpty()) {
        // kapt is applied but unused
        setOf(PluginAdvice.redundantKapt())
      } else {
        // kapt is applied and used
        emptySet()
      }
    }

    if (pluginAdvice.isNotEmpty()) {
      val adviceString = pluginAdvice.joinToString(prefix = "- ", separator = "\n- ") {
        "${it.redundantPlugin}, because ${it.reason}"
      }
      logger.debug("Redundant plugins that should be removed:\n$adviceString")
    }

    outputFile.writeText(pluginAdvice.toJson())
  }
}
