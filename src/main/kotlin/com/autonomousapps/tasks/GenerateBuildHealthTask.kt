package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateBuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates json report for build health"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var projectHealthReports: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val consoleOutput: RegularFileProperty

  @get:OutputFile
  abstract val outputFail: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()
    val consoleOutput = consoleOutput.getAndDelete()
    val outputFail = outputFail.getAndDelete()

    var didWrite = false
    var shouldFail = false

    val buildHealth: Set<ProjectAdvice> = projectHealthReports.dependencies.asSequence()
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      // we sort here because of the onEach below, where we stream the console output to disk
      .sortedBy { it.dependencyProject.path }
      .map { dependency ->
        projectHealthReports.fileCollection(dependency)
          .singleOrNull { it.exists() }
          ?.fromJson<ProjectAdvice>()
          // There is often no file in the root project, but we'd like it in the json report anyway
          ?: ProjectAdvice(dependency.dependencyProject.path)
      }
      .onEach { projectAdvice ->
        if (projectAdvice.isNotEmpty()) {
          shouldFail = shouldFail || projectAdvice.shouldFail

          val report = ProjectHealthConsoleReportBuilder(projectAdvice).text
          consoleOutput.appendText("Advice for ${projectAdvice.projectPath}\n$report\n\n")
          didWrite = true
        }
      }
      .toSortedSet()

    output.writeText(buildHealth.toJson())
    outputFail.writeText(shouldFail.toString())
    if (!didWrite) {
      // This file must always exist, even if empty
      consoleOutput.writeText("")
    }
  }
}
