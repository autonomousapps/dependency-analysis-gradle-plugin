// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("unused")

package com.autonomousapps

import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.ProjectAdvice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

/** Extend this class to do custom post-processing of the [ProjectAdvice] produced by this project. */
@DisableCachingByDefault
public abstract class AbstractPostProcessingTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
  }

  @get:PathSensitive(PathSensitivity.NONE)
  @get:InputFile
  public abstract val input: RegularFileProperty

  public fun projectAdvice(): ProjectAdvice = input.fromJson()
}
