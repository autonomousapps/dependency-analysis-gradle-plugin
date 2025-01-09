package com.autonomousapps.tasks

import com.autonomousapps.internal.graph.GraphViewBuilder
import com.autonomousapps.internal.graph.GraphWriter
import com.autonomousapps.internal.graph.stripVariants
import com.autonomousapps.internal.graph.plus
import com.autonomousapps.internal.utils.GraphAdapter.GraphContainer
import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateProjectGraphTask : DefaultTask() {

  init {
    description = "Generates several graph views of this project's local dependency graph"
  }

  internal companion object {
    const val PROJECT_COMBINED_CLASSPATH_JSON = "project-combined-classpath.json"
    const val PROJECT_COMPILE_CLASSPATH_GV = "project-compile-classpath.gv"
    const val PROJECT_RUNTIME_CLASSPATH_GV = "project-runtime-classpath.gv"
    const val PROJECT_COMBINED_CLASSPATH_GV = "project-combined-classpath.gv"
  }

  @get:Input
  abstract val buildPath: Property<String>

  @get:Input
  abstract val compileClasspath: Property<ResolvedComponentResult>

  @get:Input
  abstract val runtimeClasspath: Property<ResolvedComponentResult>

  @get:OutputDirectory
  abstract val output: DirectoryProperty

  @TaskAction fun action() {
    val compileOutput = output.file(PROJECT_COMPILE_CLASSPATH_GV).getAndDelete()
    val runtimeOutput = output.file(PROJECT_RUNTIME_CLASSPATH_GV).getAndDelete()
    val combinedOutput = output.file(PROJECT_COMBINED_CLASSPATH_GV).getAndDelete()
    val combinedJsonOutput = output.file(PROJECT_COMBINED_CLASSPATH_JSON).getAndDelete()
    val compileTopOutput = output.file("project-compile-classpath-topological.txt").getAndDelete()
    val runtimeTopOutput = output.file("project-runtime-classpath-topological.txt").getAndDelete()

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

    val graphWriter = GraphWriter(buildPath.get())
    val buildPath = buildPath.get()

    // Write graphs
    compileOutput.writeText(graphWriter.toDot(compileGraph))
    runtimeOutput.writeText(graphWriter.toDot(runtimeGraph))

    // Write out combined compile + runtime graph
    val combinedGraph = compileGraph.stripVariants(buildPath) + runtimeGraph.stripVariants(buildPath)
    combinedOutput.writeText(graphWriter.toDot(combinedGraph))
    combinedJsonOutput.bufferWriteJson(GraphContainer(combinedGraph))

    // Write topological sorts
    compileTopOutput.writeText(graphWriter.topological(compileGraph))
    runtimeTopOutput.writeText(graphWriter.topological(runtimeGraph))
  }
}
