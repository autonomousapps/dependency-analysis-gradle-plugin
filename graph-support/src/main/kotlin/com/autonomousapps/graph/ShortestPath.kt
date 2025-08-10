// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.graph

import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import java.util.*

/** With thanks to Algorithms, 4th Ed. */
@Suppress("UnstableApiUsage") // Guava graphs
public class ShortestPath<N>(
  graph: Graph<N>,
  private val source: N
) where N : Any {

  private val distTo = linkedMapOf<N, Float>()
  private val edgeTo = linkedMapOf<N, EndpointPair<N>>()

  init {
    for (node in graph.nodes()) {
      distTo[node] = Float.POSITIVE_INFINITY
    }
    distTo[source] = 0f

    // visit nodes in topological order
    val top = Topological(graph, source)
    for (from in top.order) {
      for (to in graph.successors(from)) {
        relax(from, to)
      }
    }
  }

  /** Returns the distance from [source] to [other] if there is a path, otherwise null. */
  public fun distanceTo(other: N): Int? {
    return distTo[other]?.toInt()
  }

  public fun hasPathTo(other: N): Boolean {
    val dist = distTo[other] ?: return false
    return dist < Float.MAX_VALUE
  }

  public fun pathTo(other: N): Iterable<N> {
    if (!hasPathTo(other)) return emptyList()

    // Flatten the list of edges into a list of nodes
    return edgesTo(other).mapTo(mutableListOf(source)) {
      it.target()
    }
  }

  private fun edgesTo(other: N): Iterable<EndpointPair<N>> {
    if (!hasPathTo(other)) return emptyList()

    val path = ArrayDeque<EndpointPair<N>>()
    var e = edgeTo[other]
    while (e != null) {
      path.push(e)
      e = edgeTo[e.source()]
    }
    return path
  }

  private fun relax(source: N, target: N) {
    if (distTo[target]!! > distTo[source]!! + 1) {
      distTo[target] = distTo[source]!! + 1
      edgeTo[target] = EndpointPair.ordered(source, target)
    }
  }
}
