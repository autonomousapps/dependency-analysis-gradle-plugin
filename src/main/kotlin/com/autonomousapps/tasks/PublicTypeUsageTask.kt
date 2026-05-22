// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.internal.utils.readText
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@UntrackedTask(because = "Always prints output")
public abstract class PublicTypeUsageTask @Inject constructor(
  private val workerExecutor: WorkerExecutor,
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates a global report of public type usage"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val consoleReport: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(Action::class.java) {
      it.consoleReport.set(consoleReport)
    }
  }

  public interface Parameters : WorkParameters {
    public val consoleReport: RegularFileProperty
  }

  public abstract class Action : WorkAction<Parameters> {

    private val logger = getLogger<PublicTypeUsageTask>()

    override fun execute() {
      logger.quiet(parameters.consoleReport.readText())
    }
  }
}
