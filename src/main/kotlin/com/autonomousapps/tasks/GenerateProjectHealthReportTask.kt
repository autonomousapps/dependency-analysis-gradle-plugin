// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.extension.ReportingHandler
import com.autonomousapps.extension.getEffectivePostscript
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.advice.ProjectHealthSarifReportBuilder
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.getAndDeleteNullable
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.SourcedProjectAdvice
import com.autonomousapps.model.internal.ProjectMetadata
import io.github.detekt.sarif4k.SarifSerializer
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

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  @get:Optional
  public abstract val sourcedProjectAdvice: RegularFileProperty

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val projectMetadata: RegularFileProperty

  // TODO(tsr): this shouldn't be a Property for Complicated Reasons
  @get:Nested
  public abstract val reportingConfig: Property<ReportingHandler.Config>

  @get:Input
  public abstract val dslKind: Property<DslKind>

  @get:Input
  public abstract val dependencyMap: MapProperty<String, String>

  @get:Input
  public abstract val useTypesafeProjectAccessors: Property<Boolean>

  @get:Input
  public abstract val useParenthesesForGroovy: Property<Boolean>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  @get:Optional
  public abstract val sarifOutput: RegularFileProperty

  @TaskAction public fun action() {
    workerExecutor.noIsolation().submit(ProjectHealthAction::class.java) {
      it.advice.set(projectAdvice)
      it.sourcedAdvice.set(sourcedProjectAdvice)
      it.projectMetadata.set(projectMetadata)
      it.reportingConfig.set(reportingConfig)
      it.dslKind.set(dslKind)
      it.dependencyMap.set(dependencyMap)
      it.useTypesafeProjectAccessors.set(useTypesafeProjectAccessors)
      it.useParenthesesForGroovy.set(useParenthesesForGroovy)
      it.output.set(output)
      it.sarifOutput.set(sarifOutput)
    }
  }

  public interface ProjectHealthParameters : WorkParameters {
    public val advice: RegularFileProperty
    public val sourcedAdvice: RegularFileProperty
    public val projectMetadata: RegularFileProperty
    public val reportingConfig: Property<ReportingHandler.Config>
    public val dslKind: Property<DslKind>
    public val dependencyMap: MapProperty<String, String>
    public val useTypesafeProjectAccessors: Property<Boolean>
    public val useParenthesesForGroovy: Property<Boolean>
    public val output: RegularFileProperty
    public val sarifOutput: RegularFileProperty
  }

  public abstract class ProjectHealthAction : WorkAction<ProjectHealthParameters> {

    override fun execute() {
      val output = parameters.output.getAndDelete()

      val projectAdvice = parameters.advice.fromJson<ProjectAdvice>()
      val projectMetadata = parameters.projectMetadata.fromJson<ProjectMetadata>()

      val dslKind = parameters.dslKind.get()
      val dependencyMap = parameters.dependencyMap.get().toLambda()
      val useTypesafeProjectAccessors = parameters.useTypesafeProjectAccessors.get()

      val consoleText = ProjectHealthConsoleReportBuilder(
        projectAdvice = projectAdvice,
        projectMetadata = projectMetadata,
        postscript = parameters.reportingConfig.get().getEffectivePostscript(projectAdvice.shouldFail),
        dslKind = dslKind,
        dependencyMap = dependencyMap,
        useTypesafeProjectAccessors = useTypesafeProjectAccessors,
        useParenthesesForGroovy = parameters.useParenthesesForGroovy.get(),
      ).text

      output.writeText(consoleText)

      val sarifOutput = parameters.sarifOutput.getAndDeleteNullable()
      if (sarifOutput != null) {
        val sourcedAdvice = parameters.sourcedAdvice.fromJson<SourcedProjectAdvice>()
        val sarifText = ProjectHealthSarifReportBuilder(
          listOf(sourcedAdvice),
          parameters.dslKind.get(),
          dependencyMap,
          useTypesafeProjectAccessors
        ).sarif

        sarifOutput.writeText(SarifSerializer.toJson(sarifText))
      }

    }
  }
}
