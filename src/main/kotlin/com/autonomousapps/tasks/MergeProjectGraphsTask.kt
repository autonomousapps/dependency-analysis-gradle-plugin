// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.graph.plus
import com.autonomousapps.internal.utils.GraphAdapter.GraphContainer
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
public abstract class MergeProjectGraphsTask : DefaultTask() {

  init {
    description = "Merges the project graphs of all variants into a single graph"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val projectGraphs: ListProperty<RegularFile>

  @get:OutputFile
  public abstract val output: RegularFileProperty

  @TaskAction public fun action() {
    val output = output.getAndDelete()

    val graph = projectGraphs.get()
      .map { it.fromJson<GraphContainer>().graph }
      .reduce { acc, graph -> acc + graph }

    output.bufferWriteJson(GraphContainer(graph))
  }
}
