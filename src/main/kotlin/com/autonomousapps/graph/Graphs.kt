@file:Suppress("UnstableApiUsage") // Guava Graph API

package com.autonomousapps.graph

import com.autonomousapps.internal.utils.filterNotToSet
import com.autonomousapps.model.Coordinates
import com.google.common.graph.EndpointPair
import com.google.common.graph.Graph
import com.google.common.graph.Traverser
import java.util.ArrayDeque
import com.google.common.graph.Graphs as GuavaGraphs

internal object Graphs {

  fun Graph<Coordinates>.reachableNodes(node: Coordinates): Set<Coordinates> {
    return GuavaGraphs.reachableNodes(this, node)
      // exclude self from list
      .filterNotToSet { it == node }
  }

  fun Graph<Coordinates>.children(node: Coordinates): Set<Coordinates> = successors(node)

  fun Graph<Coordinates>.shortestPath(source: Coordinates, target: Coordinates): Iterable<Coordinates> {
    val path = ShortestPath(this, source)
    return path.pathTo(target)
  }

  /*
   * The use of `where N : Any` is solely to suppress the following compilation warning:
   *
   * "Type mismatch: value of a nullable type N is used where non-nullable type is expected. This warning will become an
   * error soon."
   *
   * This is silly and presumably happens because Guava's Graph APIs don't have nullability annotations.
   */

  /** With thanks to Algorithms, 4th Ed. */
  internal class ShortestPath<N>(
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

    fun hasPathTo(other: N): Boolean {
      val dist = distTo[other] ?: return false
      return dist < Float.MAX_VALUE
    }

    fun pathTo(other: N): Iterable<N> {
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

  /** With thanks to Algorithms, 4th Ed. See p582 for the explanation for why we want the reverse postorder. */
  internal class Topological<N>(
    graph: Graph<N>,
    source: N
  ) where N : Any {

    val order: Iterable<N>

    init {
      val postorder: Iterable<N> = Traverser.forGraph(graph).depthFirstPostOrder(source)
      val reverse = ArrayDeque<N>()
      for (node in postorder) reverse.push(node)
      order = reverse
    }
  }
}
