package com.autonomousapps.internal.graph

import com.autonomousapps.internal.utils.filterNotToSet
import com.google.common.graph.Graph
import com.google.common.graph.Graphs as GuavaGraphs

@Suppress("UnstableApiUsage") // Guava graphs
internal object Graphs {

  fun <N : Any> Graph<N>.reachableNodes(node: N, excludeSelf: Boolean = true): Set<N> {
    val reachable = GuavaGraphs.reachableNodes(this, node)
    return if (excludeSelf) {
      reachable.filterNotToSet { it == node }
    } else {
      reachable
    }
  }

  fun <N : Any> Graph<N>.parents(node: N): Set<N> = predecessors(node)

  fun <N : Any> Graph<N>.children(node: N): Set<N> = successors(node)

  fun <N : Any> Graph<N>.shortestPath(source: N, target: N): Iterable<N> {
    val path = ShortestPath(this, source)
    return path.pathTo(target)
  }

  fun <N : Any> Graph<N>.topological(source: N): Iterable<N> = Topological(this, source).order
}
