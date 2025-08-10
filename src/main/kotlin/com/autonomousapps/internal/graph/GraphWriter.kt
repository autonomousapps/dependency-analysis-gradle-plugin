// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.graph

import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.graph.Graphs.topological
import com.autonomousapps.internal.utils.appendReproducibleNewLine
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.Traverser

@Suppress("UnstableApiUsage")
internal class GraphWriter(private val buildPath: String) {

  private companion object {
    // TODO(tsr): similar code in moshi.kt
    val EDGE_COMPARATOR: Comparator<EndpointPair<Coordinates>> = Comparator { left, right ->
      compareBy(EndpointPair<Coordinates>::source)
        .thenComparing(EndpointPair<Coordinates>::target)
        .compare(left, right)
    }
  }

  fun toDot(graph: Graph<Coordinates>): String = buildString {
    val projectNodes = graph.nodes().asSequence()
      // Maybe transform an IncludedBuildCoordinates into its resolvedProject for more human-readable reporting
      .map { it.maybeProjectCoordinates(buildPath) }
      .filterIsInstance<ProjectCoordinates>()
      .map { it.gav() }
      .sorted()
      .toList()

    appendReproducibleNewLine("strict digraph DependencyGraph {")
    appendReproducibleNewLine("  ratio=0.6;")
    appendReproducibleNewLine("  node [shape=box];")

    // styling for project nodes
    if (projectNodes.isNotEmpty()) appendReproducibleNewLine()
    projectNodes.forEach {
      appendReproducibleNewLine("  \"$it\" [style=filled fillcolor=\"#008080\"];")
    }
    if (projectNodes.isNotEmpty()) appendReproducibleNewLine()

    // the graph itself
    graph.edges()
      .sortedWith(EDGE_COMPARATOR)
      .forEach { edge ->
        val source = edge.nodeU().maybeProjectCoordinates(buildPath)
        val target = edge.nodeV().maybeProjectCoordinates(buildPath)
        val style =
          if (source is ProjectCoordinates && target is ProjectCoordinates) " [style=bold color=\"#FF6347\" weight=8]"
          else ""
        append("  \"${source.gav()}\" -> \"${target.gav()}\"$style;")
        append("\n")
      }
    append("}")
  }

  /**
   * Returns the [graph] sorted into topological order, ascending. Each node in the graph is paired with its depth.
   *
   * TODO(tsr): shortest path is wrong, I need the _longest_ path.
   */
  fun topological(graph: Graph<Coordinates>): String {
    val root = graph.root()
    val top = graph.topological(root)
    // val paths = graph.shortestPaths(root)

    return buildString {
      top.forEach { node ->
        appendLine(node.maybeProjectCoordinates(buildPath).gav())
        // append(" ")
        // appendLine("${paths.distanceTo(node)}")
      }
    }
  }

  // TODO replace with Hu's algorithm (for scheduling concurrent work), and push relevant bits into Graphs.kt.
  fun workPlan(graph: Graph<Coordinates>): String {
    val traverser = Traverser.forGraph(graph)
    val topological = graph.topological(graph.root()).toList()
    val plan = mutableListOf<MutableList<Coordinates>>()

    var list = mutableListOf(topological.first())
    topological
      .drop(1)
      .forEach { node ->
        val hasPathTo = list.any { traverser.breadthFirst(it).contains(node) }
        if (!hasPathTo) {
          list.add(node)
        } else {
          plan.add(list)
          list = mutableListOf(node)
        }
      }

    // add final list
    plan.add(list)

    return buildString {
      plan.forEachIndexed { i, batch ->
        appendLine("$i (${batch.size} items)")
        batch.forEach { node ->
          append(" ")
          appendLine(node.maybeProjectCoordinates(buildPath).gav())
        }
      }
    }
  }
}
