package com.autonomousapps.tasks

import com.autonomousapps.TASK_GROUP_DEP
import com.autonomousapps.graph.*
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.getAndDelete
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option

/**
 * This task generates a complete project-dependencies graph for every subproject in a build, as
 * well as a reverse-dependencies graph which shows which projects might be impacted by a change in
 * another project.
 */
@CacheableTask
abstract class ProjectGraphAggregationTask : DefaultTask() {

  init {
    group = TASK_GROUP_DEP
    description = "Produces a graph of all inter-project dependencies"
  }

  private var query: String = ""

  @Option(option = "id", description = "The project dependency for which to generate a reverse graph")
  fun query(identifier: String) {
    this.query = identifier
  }

  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:InputFiles
  lateinit var graphs: Configuration

  @get:OutputFile
  abstract val output: RegularFileProperty

  @get:OutputFile
  abstract val outputRev: RegularFileProperty

  @get:OutputFile
  abstract val outputRevSub: RegularFileProperty

  @TaskAction fun action() {
    val outputFile = output.getAndDelete()
    val outputRevFile = outputRev.getAndDelete()
    val outputRevSubFile = outputRevSub.getAndDelete()

    val graph = graphs.dependencies
      .filterIsInstance<ProjectDependency>()
      .flatMap { dep ->
        graphs.fileCollection(dep)
          .filter { it.exists() }
          .map { it.fromJson<DependencyGraph>() }
      }.merge()

    val reversed = graph.reversed()

    logger.quiet("Graph DOT at ${outputFile.path}")
    outputFile.writeText(GraphWriter.toDot(graph))

    logger.quiet("Graph rev DOT at ${outputRevFile.path}")
    outputRevFile.writeText(GraphWriter.toDot(reversed))

    if (query.isNotEmpty()) {
      val node = getQueryNode()
      val subgraph = DepthFirstSearch(reversed, node).subgraph

      logger.quiet("Subgraph rooted on $query at ${outputRevSubFile.path}")
      outputRevSubFile.writeText(GraphWriter.toDot(subgraph))
    }
  }

  private fun getQueryNode(): Node {
    if (!query.startsWith(":")) {
      throw GradleException("You cannot query for a non-project dependency.")
    }

    return ProducerNode(query)
  }
}