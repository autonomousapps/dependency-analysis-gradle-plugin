// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.DependencyAnalysisPlugin
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.extension.ReportingHandler
import com.autonomousapps.extension.getEffectivePostscript
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.advice.BuildHealthWriter
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.Colors
import com.autonomousapps.internal.utils.Colors.colorize
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.peekJsonString
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.internal.ProjectMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
public abstract class GenerateBuildHealthTask : DefaultTask() {

  init {
    description = "Generates json report for build health"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectHealthReports: ConfigurableFileCollection

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectMetadataReports: ConfigurableFileCollection

  // TODO(tsr): this shouldn't be a Property for Complicated Reasons
  @get:Nested
  public abstract val reportingConfig: Property<ReportingHandler.Config>

  /** The number of projects (modules) in this build, including the root project. */
  @get:Input
  public abstract val projectCount: Property<Int>

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
  public abstract val consoleOutput: RegularFileProperty

  @get:OutputFile
  public abstract val outputFail: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()
    val consoleOutput = consoleOutput.getAndDelete()
    val outputFail = outputFail.getAndDelete()

    val reportsByPath = sortedMapOf<String, File>().apply {
      projectHealthReports.files.forEach { file ->
        val projectPath = file.peekJsonString("projectPath")
          ?: error("No 'projectPath' in project health report '$file'.")
        put(projectPath, file)
      }
    }

    val metadata = projectMetadataReports.files.asSequence()
      .map { it.fromJson<ProjectMetadata>() }
      .associateBy { it.projectPath }

    if (isFunctionallyEmpty(reportsByPath.keys)) {
      logger.warn(
        """
          No project health reports found. Is '${DependencyAnalysisPlugin.ID}' not applied to any subprojects in this build?
          See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/wiki/Adding-to-your-project
        """.trimIndent()
      )
    }

    var didWrite = false

    val shouldFail = BuildHealthWriter(output).write(
      advice = reportsByPath.values.asSequence().map { it.fromJson<ProjectAdvice>() },
      projectCount = reportsByPath.size,
    ) { projectAdvice ->
      if (didWrite) {
        // Add separation between each set of non-empty project advice
        consoleOutput.appendText("\n\n")
      }

      val projectMetadata = metadata[projectAdvice.projectPath]
        ?: error("Missing metadata for '${projectAdvice.projectPath}'.")

      // console report
      val report = ProjectHealthConsoleReportBuilder(
        projectAdvice = projectAdvice,
        projectMetadata = projectMetadata,
        // For buildHealth, we want to include the postscript only once.
        postscript = "",
        dslKind = dslKind.get(),
        dependencyMap = dependencyMap.get().toLambda(),
        useTypesafeProjectAccessors = useTypesafeProjectAccessors.get(),
        useParenthesesForGroovy = useParenthesesForGroovy.get(),
      ).text
      val projectPath = if (projectAdvice.projectPath == ":") "root project" else projectAdvice.projectPath
      consoleOutput.appendText("Advice for ${projectPath}\n$report")
      didWrite = true
    }

    outputFail.writeText(shouldFail.toString())

    if (!didWrite) {
      // This file must always exist, even if empty
      consoleOutput.writeText("")
    } else {
      // Append postscript if it exists, and we haven't been configured to omit it for non-failures.
      val reportingConfig = reportingConfig.get()
      val ps = reportingConfig.getEffectivePostscript(shouldFail)
      if (ps.isNotEmpty()) {
        consoleOutput.appendText("\n\n${ps.colorize(Colors.BOLD)}")
      }
    }
  }

  private fun isFunctionallyEmpty(projectPaths: Collection<String>): Boolean {
    // if there's no advice, then advice is functionally empty
    if (projectPaths.isEmpty()) return true

    // if there's one piece of advice, and it's for the root project, and this build has more than one project, then
    // advice is functionally empty
    if (projectPaths.size == 1 && projectPaths.singleOrNull { it == ":" } != null) {
      return projectCount.get() != 1
    }

    return false
  }
}
