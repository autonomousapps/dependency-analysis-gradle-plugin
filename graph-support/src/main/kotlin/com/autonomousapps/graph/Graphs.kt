package com.autonomousapps.graph

import com.google.common.graph.Graph
import com.google.common.graph.Graphs as GuavaGraphs

@Suppress("UnstableApiUsage") // Guava graphs
public object Graphs {

  public fun <N : Any> Graph<N>.reachableNodes(node: N, excludeSelf: Boolean = true): Set<N> {
    val reachable = GuavaGraphs.reachableNodes(this, node)
    return if (excludeSelf) {
      reachable.filterNotTo(HashSet()) { it == node }
    } else {
      reachable
    }
  }

  public fun <N : Any> Graph<N>.parents(node: N): Set<N> = predecessors(node)

  public fun <N : Any> Graph<N>.children(node: N): Set<N> = successors(node)

  /**
   * Returns an ordered list of nodes if there is a path from [source] to [target]. If there is no path, returns an
   * empty list.
   */
  public fun <N : Any> Graph<N>.shortestPath(source: N, target: N): Iterable<N> {
    val path = ShortestPath(this, source)
    return path.pathTo(target)
  }

  public fun <N : Any> Graph<N>.topological(source: N): Iterable<N> = Topological(this, source).order
}
