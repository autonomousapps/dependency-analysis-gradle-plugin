@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Ripple
import com.autonomousapps.internal.advice.RippleWriter
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.getLogger
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

abstract class RipplesTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Emits to console all potential 'ripples' relating to dependency advice"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val ripples: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      ripples.set(this@RipplesTask.ripples)
    }
  }

  interface Parameters : WorkParameters {
    val ripples: RegularFileProperty
  }

  abstract class Action : WorkAction<Parameters> {
    private val logger = getLogger<RipplesTask>()

    override fun execute() {
      val ripples = parameters.ripples.fromJsonList<Ripple>()
      val msg = RippleWriter(ripples).buildMessage()
      logger.quiet(msg)
    }
  }
}
