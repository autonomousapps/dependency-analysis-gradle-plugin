package com.autonomousapps.internal.graph

import com.autonomousapps.internal.isJavaPlatform
import com.autonomousapps.internal.utils.rootCoordinates
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.google.common.graph.Graph
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

/**
 * Walks the resolved dependency graph to create a dependency graph rooted on the current project in a configuration
 * cache-compatible way.
 */
@Suppress("UnstableApiUsage") // Guava Graph
internal class GraphViewBuilder(
  root: ResolvedComponentResult,
  fileCoordinates: Set<Coordinates>,
) {

  val graph: Graph<Coordinates>

  private val graphBuilder = DependencyGraphView.newGraphBuilder()

  private val visited = mutableSetOf<Coordinates>()

  init {
    val rootId = root.rootCoordinates()

    walkFileDeps(fileCoordinates, rootId)
    walk(root, rootId)

    graph = graphBuilder.build()
  }

  private fun walkFileDeps(fileCoordinates: Set<Coordinates>, rootId: Coordinates) {
    graphBuilder.addNode(rootId)

    // the only way to get flat jar file dependencies
    fileCoordinates.forEach { id ->
      graphBuilder.putEdge(rootId, id)
    }
  }

  private fun walk(root: ResolvedComponentResult, rootId: Coordinates) {
    root.dependencies
      .filterIsInstance<ResolvedDependencyResult>()
      // AGP adds all runtime dependencies as constraints to the compile classpath, and these show
      // up in the resolution result. Filter them out.
      .filterNot { it.isConstraint }
      // For similar reasons as above
      .filterNot { it.isJavaPlatform() }
      // Sometimes there is a self-dependency?
      .filterNot { it.toCoordinates() == rootId }
      .forEach { dependencyResult ->
        // Might be from an included build, in which case the coordinates reflect the _requested_ dependency instead of
        // the _resolved_ dependency.
        val depId = dependencyResult.toCoordinates()

        // add an edge
        graphBuilder.putEdge(rootId, depId)

        if (!visited.contains(depId)) {
          visited.add(depId)
          // recursively walk the graph in a depth-first pattern
          walk(dependencyResult.selected, depId)
        }
      }
  }
}
