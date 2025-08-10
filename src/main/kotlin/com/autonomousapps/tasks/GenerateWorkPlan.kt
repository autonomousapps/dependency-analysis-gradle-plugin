// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.graph.Graphs.roots
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.graph.newGraphBuilder
import com.autonomousapps.internal.graph.plus
import com.autonomousapps.internal.utils.GraphAdapter.GraphContainer
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectCoordinates
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

// TODO(tsr): fix or delete
@CacheableTask
public abstract class GenerateWorkPlan : DefaultTask() {

  init {
    description = "Generates work plan for fixing dependency issues with minimal conflict"
  }

  @get:Input
  public abstract val buildPath: Property<String>

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  public abstract val combinedProjectGraphs: ConfigurableFileCollection

  @get:OutputDirectory
  public abstract val outputDirectory: DirectoryProperty

  @TaskAction public fun action() {
    val combinedGraphOut = outputDirectory.file("combined-graph.json").getAndDelete()
    val combinedGraphDotOut = outputDirectory.file("combined-graph.gz").getAndDelete()
    val workPlanJsonOut = outputDirectory.file("work-plan.json").getAndDelete()
    val workPlanTextOut = outputDirectory.file("work-plan.txt").getAndDelete()

    // TODO(tsr): this is all very ugly
    val combinedGraph = combinedProjectGraphs.files
      .map { it.fromJson<GraphContainer>().graph }
      .reduce { acc, graph -> acc + graph }

    val graphWriter = GraphWriter(buildPath.get())

    // combinedGraphDotOut.writeText(graphWriter.toDotS(combinedGraph))
    // combinedGraphOut.bufferWriteJson(GraphStringContainer(combinedGraph))

    // Need to add artificial edges from the root project to every root (generally, apps) in the build
    val rootProject = ProjectCoordinates(
      identifier = ":",
      gradleVariantIdentification = GradleVariantIdentification.EMPTY,
      buildPath = buildPath.get(),
    )
    val graphBuilder = newGraphBuilder<Coordinates>()
    combinedGraph.roots()
      .filterNot { it.identifier == ":" }
      .forEach { root ->
        graphBuilder.putEdge(ProjectCoordinates(":", GradleVariantIdentification.EMPTY), root)
      }
    val finalGraph = graphBuilder.build() + combinedGraph

    combinedGraphDotOut.writeText(graphWriter.toDot(finalGraph))
    combinedGraphOut.bufferWriteJson(GraphContainer(finalGraph))

    // TODO(tsr): this task should only generate the plan, and another task will print it.
    graphWriter.workPlan(finalGraph).also {
      logger.quiet("[INCUBATING] Work plan:\n$it")
    }
  }
}
