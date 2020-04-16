@file:Suppress("UnstableApiUsage")

package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.internal.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
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

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var advicePluginReports: Configuration

  @get:Input
  abstract val chatty: Property<Boolean>

  @get:OutputFile
  abstract val projectReport: RegularFileProperty

  @get:OutputFile
  abstract val projectReportPretty: RegularFileProperty

  private val chatter by lazy { chatter(chatty.get()) }

  @TaskAction
  fun action() {
    // Outputs
    val projectReportFile = projectReport.getAndDelete()
    val projectReportPrettyFile = projectReportPretty.getAndDelete()

    val dependencyAdvice = adviceReports.dependencies.map { dependency ->
      val path = (dependency as ProjectDependency).dependencyProject.path

      val advice = adviceReports.fileCollection(dependency)
        .singleFile
        .readText()
        .fromJsonSet<Advice>()

      path to advice
    }

    val pluginAdvice = advicePluginReports.dependencies.map { dependency ->
      val path = (dependency as ProjectDependency).dependencyProject.path

      val advice = advicePluginReports.fileCollection(dependency)
        .singleFile
        .readText()
        .fromJsonSet<PluginAdvice>()

      path to advice
    }

    val buildHealths = mutableSetOf<BuildHealth>()
    dependencyAdvice.forEach { (projectPath, advice) ->
      buildHealths.add(BuildHealth(
        projectPath = projectPath,
        dependencyAdvice = advice,
        pluginAdvice = pluginAdvice.find { it.first == projectPath }?.second ?: emptySet()
      ))
    }

    projectReportFile.writeText(buildHealths.toJson())
    projectReportPrettyFile.writeText(buildHealths.toPrettyString())

    if (dependencyAdvice.isNotEmpty() || pluginAdvice.isNotEmpty()) {
      chatter.chat("Advice report (aggregated) : ${projectReportFile.path}")
      chatter.chat("(pretty-printed)           : ${projectReportPrettyFile.path}")
    }
  }
}
