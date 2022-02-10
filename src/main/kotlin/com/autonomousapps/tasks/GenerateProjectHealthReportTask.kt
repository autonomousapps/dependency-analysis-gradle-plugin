@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class GenerateProjectHealthReportTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates console report for project health"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ProjectHealthAction::class.java) {
      advice.set(this@GenerateProjectHealthReportTask.projectAdvice)
      output.set(this@GenerateProjectHealthReportTask.output)
    }
  }

  interface ProjectHealthParameters : WorkParameters {
    val advice: RegularFileProperty
    val output: RegularFileProperty
  }

  abstract class ProjectHealthAction : WorkAction<ProjectHealthParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val projectAdvice = parameters.advice.fromJson<ProjectAdvice>()
      val consoleText = ProjectHealthConsoleReportBuilder(projectAdvice).text

      output.writeText(consoleText)
    }
  }
}
