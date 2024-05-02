package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class ProjectGraphTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Generates a graph view of this project's local dependency graph"
  }

  @get:Input
  abstract val compileClasspath: Property<ResolvedComponentResult>

  @get:Input
  abstract val runtimeClasspath: Property<ResolvedComponentResult>

  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @TaskAction fun action() {
    val compileOutput = output.file("project-compile-classpath.gv").getAndDelete()
    val runtimeOutput = output.file("project-runtime-classpath.gv").getAndDelete()

    val compileGraph = GraphViewBuilder(
      root = compileClasspath.get(),
      fileCoordinates = emptySet(),
      localOnly = true,
    ).graph

    val runtimeGraph = GraphViewBuilder(
      root = runtimeClasspath.get(),
      fileCoordinates = emptySet(),
      localOnly = true,
    ).graph

    compileOutput.writeText(GraphWriter.toDot(compileGraph))
    runtimeOutput.writeText(GraphWriter.toDot(runtimeGraph))
  }
}
