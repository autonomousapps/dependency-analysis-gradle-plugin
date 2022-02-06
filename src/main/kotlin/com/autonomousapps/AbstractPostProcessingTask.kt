@file:Suppress("unused")

package com.autonomousapps

import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Extend this class to do custom post-processing of the [ComprehensiveAdvice] produced by this project.
 */
abstract class AbstractPostProcessingTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  abstract val input: RegularFileProperty

  fun projectAdvice(): ProjectAdvice = input.fromJson()
}
