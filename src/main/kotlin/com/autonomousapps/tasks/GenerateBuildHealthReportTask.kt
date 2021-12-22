package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.fromJsonSet
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
abstract class GenerateBuildHealthReportTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates console report for build health"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val buildHealth: RegularFileProperty

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(BuildHealthAction::class.java) {
      buildHealth.set(this@GenerateBuildHealthReportTask.buildHealth)
      output.set(this@GenerateBuildHealthReportTask.output)
    }
  }

  interface BuildHealthParameters : WorkParameters {
    val buildHealth: RegularFileProperty
    val output: RegularFileProperty
  }

  abstract class BuildHealthAction : WorkAction<BuildHealthParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val buildHealth = parameters.buildHealth.fromJsonSet<ProjectAdvice>()

      val consoleText = buildHealth.asSequence()
        .map { it.projectPath to ProjectHealthConsoleReportBuilder(it).text }
        .filter { (_, report) -> report.isNotBlank() }
        .joinToString(separator = "\n\n") { (projectPath, report) ->
          "Advice for $projectPath\n$report"
        }

      output.writeText(consoleText)
    }
  }
}
