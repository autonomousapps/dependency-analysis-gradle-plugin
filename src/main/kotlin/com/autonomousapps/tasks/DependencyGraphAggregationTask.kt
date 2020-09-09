package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.graph.GraphWriter
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@CacheableTask
abstract class DependencyGraphAggregationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces the dependency graph for the current project"
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  abstract val graphs: ListProperty<RegularFile>

  @get:OutputFile
  abstract val outputJson: RegularFileProperty

  @get:OutputFile
  abstract val outputDot: RegularFileProperty

  @TaskAction fun action() {
    val outputJsonFile = outputJson.getAndDelete()
    val outputDotFile = outputDot.getAndDelete()

    val graph: DependencyGraph = graphs.get().mapToSet { it.fromJson<DependencyGraph>() }
      .reduce { acc, dependencyGraph ->
        acc.apply {
          dependencyGraph.edges().forEach {
            addEdge(it)
          }
        }
      }

    logger.quiet("Graph JSON at ${outputJsonFile.path}")
    outputJsonFile.writeText(graph.toJson())

    logger.quiet("Graph DOT at ${outputDotFile.path}")
    outputDotFile.writeText(GraphWriter.toDot(graph))
  }
}
