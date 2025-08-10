// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.utils.bufferWriteJsonMapSet
import com.autonomousapps.internal.utils.dependencyCoordinates
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.SortedSet

@CacheableTask
public abstract class ComputeDuplicateDependenciesTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Computes 'duplicate' external dependencies across entire build."
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val resolvedDependenciesReports: ConfigurableFileCollection

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val map = sortedMapOf<String, SortedSet<String>>()

    resolvedDependenciesReports
      .dependencyCoordinates()
      .forEach {
        map.merge(it.identifier, sortedSetOf(it.resolvedVersion)) { acc, inc ->
          acc.apply { addAll(inc) }
        }
      }

    output.bufferWriteJsonMapSet(map)
  }
}
