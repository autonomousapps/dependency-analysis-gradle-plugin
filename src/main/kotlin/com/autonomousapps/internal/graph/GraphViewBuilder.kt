// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.graph

import com.autonomousapps.internal.isJavaPlatform
import com.autonomousapps.internal.utils.rootCoordinates
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.google.common.graph.Graph
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
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
  private val localOnly: Boolean = false,
) {

  val graph: Graph<Coordinates>

  private val graphBuilder = DependencyGraphView.newGraphBuilder()
  private val visited = mutableSetOf<Coordinates>()
  private val componentFilter: (ResolvedDependencyResult) -> Boolean = {
    !localOnly || it.selected.id is ProjectComponentIdentifier
  }

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
    root.dependencies.asSequence()
      // Only resolved dependencies
      .filterIsInstance<ResolvedDependencyResult>()
      .filter(componentFilter)
      // AGP adds all runtime dependencies as constraints to the compile classpath, and these show
      // up in the resolution result. Filter them out.
      .filterNot { it.isConstraint }
      // For similar reasons as above
      .filterNot { it.isJavaPlatform() }
      // Sometimes there is a self-dependency?
      .filterNot { it.toCoordinates() == rootId }
      .map {
        // Might be from an included build, in which case the coordinates reflect the _requested_ dependency instead of
        // the _resolved_ dependency.
        Pair(it, it.toCoordinates())
      }
      // make reproducible output friendly to compare between executions
      .sortedWith(
        compareBy<Pair<ResolvedDependencyResult, Coordinates>> { pair -> pair.second.javaClass.simpleName }
          .thenComparing { pair -> pair.second.identifier }
      )
      .forEach { (dependencyResult, depId) ->
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
