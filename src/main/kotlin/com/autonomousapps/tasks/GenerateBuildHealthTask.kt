// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.DependencyAnalysisPlugin
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.advice.DslKind
import com.autonomousapps.internal.advice.ProjectHealthConsoleReportBuilder
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.AndroidScore
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.BuildHealth.AndroidScoreMetrics
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateBuildHealthTask : DefaultTask() {

  init {
    description = "Generates json report for build health"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val projectHealthReports: ConfigurableFileCollection

  @get:Input
  abstract val postscript: Property<String>

  /** The number of projects (modules) in this build, including the root project. */
  @get:Input
  abstract val projectCount: Property<Int>

  @get:Input
  abstract val dslKind: Property<DslKind>

  @get:Input
  abstract val dependencyMap: MapProperty<String, String>

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
    var unusedDependencies = 0
    var undeclaredDependencies = 0
    var misDeclaredDependencies = 0
    var compileOnlyDependencies = 0
    var runtimeOnlyDependencies = 0
    var processorDependencies = 0
    val androidMetricsBuilder = AndroidScoreMetrics.Builder()

    val advice = projectHealthReports.files.map { it.fromJson<ProjectAdvice>() }

    if (isFunctionallyEmpty(advice)) {
      logger.warn(
        """
          No project health reports found. Is '${DependencyAnalysisPlugin.ID}' not applied to any subprojects in this build?
          See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/wiki/Adding-to-your-project
        """.trimIndent()
      )
    }

    val projectAdvice: Set<ProjectAdvice> = advice.asSequence()
      // we sort here because of the onEach below, where we stream the console output to disk
      .sortedBy { it.projectPath }
      .onEach { projectAdvice ->
        if (projectAdvice.isNotEmpty()) {
          shouldFail = shouldFail || projectAdvice.shouldFail

          // console report
          val report = ProjectHealthConsoleReportBuilder(
            projectAdvice = projectAdvice,
            postscript = postscript.get(),
            dslKind = dslKind.get(),
            dependencyMap = dependencyMap.get().toLambda()
          ).text
          val projectPath = if (projectAdvice.projectPath == ":") "root project" else projectAdvice.projectPath
          consoleOutput.appendText("Advice for ${projectPath}\n$report\n\n")
          didWrite = true

          // counts
          projectAdvice.dependencyAdvice.forEach {
            when {
              it.isRemove() -> unusedDependencies++
              it.isAdd() -> undeclaredDependencies++
              it.isChange() -> misDeclaredDependencies++
              it.isCompileOnly() -> compileOnlyDependencies++
              it.isRuntimeOnly() -> runtimeOnlyDependencies++
              it.isProcessor() -> processorDependencies++
            }
          }
          projectAdvice.moduleAdvice.filterIsInstance<AndroidScore>().forEach {
            if (it.shouldBeJvm()) {
              androidMetricsBuilder.shouldBeJvmCount++
            } else if (it.couldBeJvm()) {
              androidMetricsBuilder.couldBeJvmCount++
            }
          }
        }
      }
      .toSortedSet()

    val buildHealth = BuildHealth(
      projectAdvice = projectAdvice,
      shouldFail = shouldFail,
      projectCount = projectAdvice.size,
      unusedCount = unusedDependencies,
      undeclaredCount = undeclaredDependencies,
      misDeclaredCount = misDeclaredDependencies,
      compileOnlyCount = compileOnlyDependencies,
      runtimeOnlyCount = runtimeOnlyDependencies,
      processorCount = processorDependencies,
      androidScoreMetrics = androidMetricsBuilder.build(),
    )

    output.bufferWriteJson(buildHealth)
    outputFail.writeText(shouldFail.toString())

    // This file must always exist, even if empty
    if (!didWrite) {
      consoleOutput.writeText("")
    }
  }

  private fun isFunctionallyEmpty(advice: Collection<ProjectAdvice>): Boolean {
    // if there's no advice, then advice is functionally empty
    if (advice.isEmpty()) return true

    // if there's one piece of advice, and it's for the root project, and this build has more than one project, then
    // advice is functionally empty
    if (advice.size == 1 && advice.singleOrNull { it.projectPath == ":" } != null) {
      return projectCount.get() != 1
    }

    return false
  }
}
