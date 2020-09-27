package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.utils.fromJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ProjectHealth : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Consumes output of aggregateAdvice and can fail the build if desired"
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val comprehensiveAdvice: RegularFileProperty

  @TaskAction fun action() {
    val comprehensiveAdvice = comprehensiveAdvice.fromJson<ComprehensiveAdvice>()

    if (comprehensiveAdvice.shouldFail) {
      throw GradleException("Dependency Analysis Gradle Plugin has detected fatal issues. Please see advice reports")
    }
  }
}
