// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.extension.ReportingHandler
import com.autonomousapps.extension.getEffectivePostscript
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
public abstract class GenerateProjectHealthReportTask @Inject constructor(
  private val workerExecutor: WorkerExecutor
) : DefaultTask() {

  init {
    description = "Generates console report for project health"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val projectAdvice: RegularFileProperty

  // TODO(tsr): this shouldn't be a Property for Complicated Reasons
  @get:Nested
  public abstract val reportingConfig: Property<ReportingHandler.Config>

  @get:Input
  public abstract val dslKind: Property<DslKind>

  @get:Input
  public abstract val dependencyMap: MapProperty<String, String>

  @get:Input
  public abstract val useTypesafeProjectAccessors: Property<Boolean>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(ProjectHealthAction::class.java) {
      it.advice.set(projectAdvice)
      it.reportingConfig.set(reportingConfig)
      it.dslKind.set(dslKind)
      it.dependencyMap.set(dependencyMap)
      it.useTypesafeProjectAccessors.set(useTypesafeProjectAccessors)
      it.output.set(output)
    }
  }

  public interface ProjectHealthParameters : WorkParameters {
    public val advice: RegularFileProperty
    public val reportingConfig: Property<ReportingHandler.Config>
    public val dslKind: Property<DslKind>
    public val dependencyMap: MapProperty<String, String>
    public val useTypesafeProjectAccessors: Property<Boolean>
    public val output: RegularFileProperty
  }

  public abstract class ProjectHealthAction : WorkAction<ProjectHealthParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val projectAdvice = parameters.advice.fromJson<ProjectAdvice>()
      val consoleText = ProjectHealthConsoleReportBuilder(
        projectAdvice = projectAdvice,
        postscript = parameters.reportingConfig.get().getEffectivePostscript(projectAdvice.shouldFail),
        dslKind = parameters.dslKind.get(),
        dependencyMap = parameters.dependencyMap.get().toLambda(),
        useTypesafeProjectAccessors = parameters.useTypesafeProjectAccessors.get(),
      ).text

      output.writeText(consoleText)
    }
  }
}
