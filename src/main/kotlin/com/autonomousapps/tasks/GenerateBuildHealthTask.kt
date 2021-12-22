package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP_INTERNAL
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateBuildHealthTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP_INTERNAL
    description = "Generates json report for build health"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var projectHealthReports: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @TaskAction fun action() {
    val output = output.getAndDelete()

    val buildHealth: Set<ProjectAdvice> = projectHealthReports.dependencies.asSequence()
      // They should all be project dependencies, but
      // https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/issues/295
      .filterIsInstance<ProjectDependency>()
      .map { dependency ->
        projectHealthReports.fileCollection(dependency)
          .singleOrNull { it.exists() }
          ?.fromJson<ProjectAdvice>()
        // There is often no file in the root project, but we'd like it in the report anyway
          ?: ProjectAdvice(dependency.dependencyProject.path)
      }
      .toSortedSet()

    output.writeText(buildHealth.toJson())
  }
}
