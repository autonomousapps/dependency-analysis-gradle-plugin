package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.extension.DependenciesHandler.Companion.toLambda
import com.autonomousapps.internal.GradleVersions
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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateBuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates json report for build health"

    if (GradleVersions.isAtLeastGradle74) {
      @Suppress("LeakingThis")
      notCompatibleWithConfigurationCache("Cannot serialize Configurations")
    }
  }

  @Transient
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var projectHealthReports: Configuration

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

    val projectAdvice: Set<ProjectAdvice> = projectHealthReports.dependencies.asSequence()
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

          // console report
          val report = ProjectHealthConsoleReportBuilder(
            projectAdvice = projectAdvice,
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
    if (!didWrite) {
      // This file must always exist, even if empty
      consoleOutput.writeText("")
    }
  }
}
