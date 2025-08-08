// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.readLines
import com.autonomousapps.internal.utils.toVersionCatalog
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ModuleCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
public abstract class ComputeResolvedDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes resolved external dependencies for all variants."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val externalDependencies: ListProperty<RegularFile>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @get:OutputFile
  public abstract val outputToml: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()
    val outputToml = outputToml.getAndDelete()

    val dependencies: Set<ModuleCoordinates> = externalDependencies.get()
      .asSequence()
      .flatMap { it.readLines() }
      .map {
        val external = Coordinates.of(it)
        check(external is ModuleCoordinates) { "ModuleCoordinates expected. Was $it." }
        external
      }
      .toSortedSet()

    output.writeText(dependencies.joinToString(separator = "\n") { it.gav() })
    outputToml.writeText(dependencies.toVersionCatalog())
  }
}
