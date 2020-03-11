@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.fromJsonList
import com.autonomousapps.internal.toJson
import com.autonomousapps.internal.toPrettyString
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceAggregateReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Aggregates advice reports across all subprojects"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var adviceReports: Configuration

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

    val adviceReports = adviceReports.dependencies.map { dependency ->
      val path = (dependency as ProjectDependency).dependencyProject.path

      val advice = adviceReports.fileCollection(dependency)//.files
          .singleFile
          .readText()
          .fromJsonList<Advice>()

      path to advice
    }.toMap()

    projectReportFile.writeText(adviceReports.toJson())
    projectReportPrettyFile.writeText(adviceReports.toPrettyString())

    if (adviceReports.isNotEmpty()) {
      logger.quiet("Advice report (aggregated) : ${projectReportFile.path}")
      logger.quiet("(pretty-printed)           : ${projectReportPrettyFile.path}")
    }
  }
}
