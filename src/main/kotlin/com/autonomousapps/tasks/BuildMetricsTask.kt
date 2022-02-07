package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.ProjectMetrics
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/** Metrics at the whole-build level. */
@CacheableTask
abstract class BuildMetricsTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Calculates metrics for reporting by buildHealth"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var metrics: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()

    val projMetricsMap = metrics.dependencies
      .filterIsInstance<ProjectDependency>()
      .mapNotNull { proj ->
        val path = proj.dependencyProject.path
        val metrics = metrics.fileCollection(proj)
          .filter { it.exists() }
          .singleOrNull()
          ?.readText()
          ?.fromJson<ProjectMetrics>()

        if (metrics != null) path to metrics else null
      }.toMap()

    outputFile.writeText(projMetricsMap.toJson())
  }
}
