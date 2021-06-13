package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.graph.DepthFirstSearch
import com.autonomousapps.graph.GraphWriter
import com.autonomousapps.graph.Node
import com.autonomousapps.graph.ProducerNode
import com.autonomousapps.internal.graph.mergedGraphFrom
import com.autonomousapps.internal.utils.getAndDelete
import com.autonomousapps.internal.utils.toJson
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * This task generates a complete dependencies graph for every subproject in the build, as well as a
 * reverse-dependencies graph which shows which projects might be impacted by a change in another
 * project.
 */
@CacheableTask
abstract class DependencyGraphAllProjects : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a graph of all inter-project dependencies"
  }

  private var query: String = ""

  @Option(
    option = "id",
    description = "The project dependency for which to generate a reverse graph"
  )
  fun query(identifier: String) {
    this.query = identifier
  }

  @Input
  fun getQuery(): String {
    return query
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var graphs: Configuration

  @get:OutputFile
  abstract val outputFullGraphJson: RegularFileProperty

  @get:OutputFile
  abstract val outputFullGraphDot: RegularFileProperty

  @get:OutputFile
  abstract val outputRevGraphJson: RegularFileProperty

  @get:OutputFile
  abstract val outputRevGraphDot: RegularFileProperty

  @get:OutputFile
  abstract val outputRevSubGraphDot: RegularFileProperty

  @TaskAction fun action() {
    val outputFullGraphJsonFile = outputFullGraphJson.getAndDelete()
    val outputDotFile = outputFullGraphDot.getAndDelete()
    val outputRevJsonFile = outputRevGraphJson.getAndDelete()
    val outputRevDotFile = outputRevGraphDot.getAndDelete()
    val outputRevSubDotFile = outputRevSubGraphDot.getAndDelete()

    val mergedGraph = mergedGraphFrom(graphs)
    val mergedReversedGraph = mergedGraph.reversed()

    outputFullGraphJsonFile.writeText(mergedGraph.toJson())
    outputRevJsonFile.writeText(mergedReversedGraph.toJson())

    // TODO need to run graphviz automatically
    logger.debug("Graph DOT at ${outputDotFile.path}")
    outputDotFile.writeText(GraphWriter.toDot(mergedGraph))

    logger.debug("Graph rev DOT at ${outputRevDotFile.path}")
    outputRevDotFile.writeText(GraphWriter.toDot(mergedReversedGraph))

    if (query.isNotEmpty()) {
      val node = getQueryNode()
      val subgraph = DepthFirstSearch(mergedReversedGraph, node).subgraph

      logger.quiet("Subgraph rooted on $query at ${outputRevSubDotFile.path}")
      outputRevSubDotFile.writeText(GraphWriter.toDot(subgraph))
    }
  }

  private fun getQueryNode(): Node {
    if (!query.startsWith(":")) {
      throw GradleException("You cannot query for a non-project dependency.")
    }

    return ProducerNode(query)
  }
}