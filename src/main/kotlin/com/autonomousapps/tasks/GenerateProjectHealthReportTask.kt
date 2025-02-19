// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.extension.ReportingHandler
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
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
    description = "Generates console report for project health"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val projectAdvice: RegularFileProperty

  @get:Nested
  abstract val reportingConfig: Property<ReportingHandler.Config>

  @get:Input
  abstract val dslKind: Property<DslKind>

  @get:Input
  abstract val dependencyMap: MapProperty<String, String>

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    workerExecutor.noIsolation().submit(ProjectHealthAction::class.java) {
      advice.set(this@GenerateProjectHealthReportTask.projectAdvice)
      // reportingConfig.set(this@GenerateProjectHealthReportTask.reportingConfig)
      onlyOnFailure.set(this@GenerateProjectHealthReportTask.reportingConfig.flatMap { it.onlyOnFailure })
      postscript.set(this@GenerateProjectHealthReportTask.reportingConfig.flatMap { it.postscript })
      dslKind.set(this@GenerateProjectHealthReportTask.dslKind)
      dependencyMap.set(this@GenerateProjectHealthReportTask.dependencyMap)
      output.set(this@GenerateProjectHealthReportTask.output)
    }
  }

  interface ProjectHealthParameters : WorkParameters {
    val advice: RegularFileProperty
    // TODO(tsr): this is annoying that this doesn't work. Asked Gradle about it.
    // val reportingConfig: Property<ReportingHandler.Config>
    val onlyOnFailure: Property<Boolean>
    val postscript: Property<String>
    val dslKind: Property<DslKind>
    val dependencyMap: MapProperty<String, String>
    val output: RegularFileProperty
  }

  abstract class ProjectHealthAction : WorkAction<ProjectHealthParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val reportingConfig = ReportingHandler.Config(
        onlyOnFailure = parameters.onlyOnFailure,
        postscript = parameters.postscript,
      )

      val projectAdvice = parameters.advice.fromJson<ProjectAdvice>()
      val consoleText = ProjectHealthConsoleReportBuilder(
        projectAdvice = projectAdvice,
        // postscript = parameters.reportingConfig.get().getEffectivePostscript(projectAdvice.shouldFail),
        postscript = reportingConfig.getEffectivePostscript(projectAdvice.shouldFail),
        dslKind = parameters.dslKind.get(),
        dependencyMap = parameters.dependencyMap.get().toLambda(),
      ).text

      output.writeText(consoleText)
    }
  }
}
