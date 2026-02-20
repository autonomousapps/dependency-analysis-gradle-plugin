// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.model.internal.ProjectType
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.internal.ProjectMetadata
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class WriteProjectMetadataTask : DefaultTask() {

  init {
    description = "Stores the project type so the output of 'generateBuildHealth' is useful"
  }

  @get:Input
  public abstract val projectPath: Property<String>

  @get:Input
  public abstract val projectType: Property<ProjectType>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val metadata = ProjectMetadata(
      projectPath = projectPath.get(),
      projectType = projectType.get(),
    )

    output.writeText(metadata.toJson())
  }
}
