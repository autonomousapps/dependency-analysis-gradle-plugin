@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class StrictAdviceTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Aggregates strict advice reports across all subprojects"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var adviceAllReports: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputPretty: RegularFileProperty

  @TaskAction
  fun action() {
    val outputFile = output.getAndDelete()
    val outputPrettyFile = outputPretty.getAndDelete()

    val buildHealth = adviceAllReports.dependencies
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      .mapToOrderedSet { dependency ->
        adviceAllReports.fileCollection(dependency)
          .singleOrNull { it.exists() }
          ?.fromJson<ComprehensiveAdvice>()
          // There is often no file in the root project, but we'd like it in the report anyway
          ?: ComprehensiveAdvice(dependency.dependencyProject.path)
      }

    outputFile.writeText(buildHealth.toJson())
    outputPrettyFile.writeText(buildHealth.toPrettyString())

    if (buildHealth.any { it.isNotEmpty() }) {
      logger.debug("Build health report (aggregated) : ${outputFile.path}")
      logger.debug("(pretty-printed)                 : ${outputPrettyFile.path}")
    }
  }
}
