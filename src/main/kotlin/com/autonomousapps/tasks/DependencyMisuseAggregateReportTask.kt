@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.UnusedDirectComponent
import com.autonomousapps.internal.utils.fromJsonList
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.internal.utils.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class DependencyMisuseAggregateReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Aggregates dependency misuse reports across all subprojects"
  }

  @PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var unusedDependencyReports: Configuration

  @get:OutputFile
  abstract val projectReport: RegularFileProperty

  @get:OutputFile
  abstract val projectReportPretty: RegularFileProperty

  @TaskAction
  fun action() {
    // Outputs
    val projectReportFile = projectReport.get().asFile
    val projectReportPrettyFile = projectReportPretty.get().asFile
    // Cleanup prior execution
    projectReportFile.delete()
    projectReportPrettyFile.delete()

    val unusedDirectDependencies = unusedDependencyReports.dependencies.map { dependency ->
      val path = (dependency as ProjectDependency).dependencyProject.path

      val unusedDependencies = unusedDependencyReports.fileCollection(dependency).files
          .first()
          .readText().fromJsonList<UnusedDirectComponent>()

      path to unusedDependencies
    }.toMap()

    projectReportFile.writeText(unusedDirectDependencies.toJson())
    projectReportPrettyFile.writeText(unusedDirectDependencies.toPrettyString())

    logger.debug("Unused dependencies report: ${projectReportFile.path}")
    logger.debug("Unused dependencies report, pretty-printed: ${projectReportPrettyFile.path}")
  }
}
