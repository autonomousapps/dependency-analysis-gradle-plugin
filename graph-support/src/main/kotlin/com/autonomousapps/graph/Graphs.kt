package com.autonomousapps.graph

import com.google.common.graph.Graph
import com.google.common.graph.Graphs as GuavaGraphs

@Suppress("UnstableApiUsage") // Guava graphs
public object Graphs {

  /**
   * Returns all nodes in this graph that are reachable from [node], optionally including `node` if [excludeSelf] is
   * `false` (it is `true`, or excluded, by default).
   */
  public fun <N : Any> Graph<N>.reachableNodes(node: N, excludeSelf: Boolean = true): Set<N> {
    val reachable = GuavaGraphs.reachableNodes(this, node)
    return if (excludeSelf) {
      reachable.filterNotTo(HashSet()) { it == node }
    } else {
      reachable
    }
  }

  /**
   * Returns the first node it finds that has an in-degree of 0. This is the root node if this DAG contains only one
   * such node.
   */
  public fun <N : Any> Graph<N>.root(): N = nodes().first {
    inDegree(it) == 0
  }

  /** Returns the nodes in this graph that are immediate [predecessors][Graph.predecessors] to [node]. */
  public fun <N : Any> Graph<N>.parents(node: N): Set<N> = predecessors(node)

  /** Returns the nodes in this graph that are immediate [successors][Graph.successors] to [node]. */
  public fun <N : Any> Graph<N>.children(node: N): Set<N> = successors(node)

  /**
   * Returns an ordered list of nodes if there is a path from [source] to [target]. If there is no path, returns an
   * empty list.
   */
  public fun <N : Any> Graph<N>.shortestPath(source: N, target: N): Iterable<N> {
    val path = ShortestPath(this, source)
    return path.pathTo(target)
  }

  /**
   * Returns the nodes of this graph in topological order.
   *
   * @see <a href="https://en.wikipedia.org/wiki/Topological_sorting">Topological sorting</a>
   */
  public fun <N : Any> Graph<N>.topological(source: N): Iterable<N> = Topological(this, source).order
}
