@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.advice.Rippler
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class AdviceAggregateReportTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Aggregates advice reports across all subprojects"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var adviceAllReports: Configuration

  @get:OutputFile
  abstract val projectReport: RegularFileProperty

  @get:OutputFile
  abstract val projectReportPretty: RegularFileProperty

  @get:OutputFile
  abstract val rippleReport: RegularFileProperty

  @TaskAction
  fun action() {
    // Output
    val projectReportFile = projectReport.getAndDelete()
    val projectReportPrettyFile = projectReportPretty.getAndDelete()
    val rippleFile = rippleReport.getAndDelete()

    val compAdvice = adviceAllReports.dependencies
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      .map { dependency ->
        val path = dependency.dependencyProject.path

        val compAdvice: Set<ComprehensiveAdvice> = adviceAllReports.fileCollection(dependency)
          .filter { it.exists() }
          .mapToSet { it.readText().fromJson() }

        path to compAdvice.toMutableSet()
      }.mergedMap()

    val buildHealth = compAdvice.map { (path, advice) ->
      ComprehensiveAdvice(
        projectPath = path,
        dependencyAdvice = advice.flatMapToSet { it.dependencyAdvice },
        pluginAdvice = advice.flatMapToSet { it.pluginAdvice },
        shouldFail = advice.any { it.shouldFail }
      )
    }

    val ripples = Rippler(buildHealth).computeRipples()

    projectReportFile.writeText(buildHealth.toJson())
    projectReportPrettyFile.writeText(buildHealth.toPrettyString())
    rippleFile.writeText(ripples.toJson())

    if (buildHealth.any { it.isNotEmpty() }) {
      logger.debug("Build health report (aggregated) : ${projectReportFile.path}")
      logger.debug("(pretty-printed)                 : ${projectReportPrettyFile.path}")
    }
  }
}
