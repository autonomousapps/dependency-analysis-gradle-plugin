// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.dependencyCoordinates
import com.autonomousapps.internal.utils.toVersionCatalog
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
public abstract class ComputeAllDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates a version catalog file (allLibs.versions.toml) containing all dependencies in the project."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val resolvedDependenciesReports: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction
  public fun action() {
    val outputFile = output.getAndDelete()

    val libs: Set<ModuleCoordinates> = resolvedDependenciesReports.dependencyCoordinates()

    outputFile.writeText(libs.toVersionCatalog())

    logger.quiet("Generated version catalog for all dependencies, containing ${libs.size} entries:\n${outputFile.absolutePath} ")
  }
}
