package com.autonomousapps.graph

import java.util.*

internal class ShortestPath(
  graph: DependencyGraph,
  source: Node
) {

  private val distTo = linkedMapOf<String, Float>()
  private val edgeTo = linkedMapOf<String, Edge>()

  init {
    if (!graph.hasNode(source)) {
      missingNode(source)
    }

    for (node in graph.nodes()) {
      distTo[node.identifier] = Float.POSITIVE_INFINITY
    }
    distTo[source.identifier] = 0f

    // visit vertices in topological order
    val top = Topological(graph)
    for (node in top.order()) {
      for (edge in graph.adj(node)) {
        relax(edge)
      }
    }
  }

  fun distTo(other: Node): Float = distTo[other.identifier] ?: missingNode(other)

  fun hasPathTo(other: Node): Boolean = hasPathTo(other.identifier)

  fun hasPathTo(other: String): Boolean {
    //distTo.computeIfAbsent(other.identifier) { Int.MAX_VALUE }
    return distTo[other]!! < Int.MAX_VALUE
  }

  fun pathTo(other: Node): Iterable<Edge>? = pathTo(other.identifier)

  fun pathTo(other: String): Iterable<Edge>? {
    if (!hasPathTo(other)) return null

    val path = Stack<Edge>()
    var e = edgeTo[other]
    while (e != null) {
      path.push(e)
      e = edgeTo[e.from.identifier]
    }
    return path
  }

  private fun relax(edge: Edge) {
    val v = edge.from
    val w = edge.to

    if (distTo[w.identifier]!! > distTo[v.identifier]!! + edge.weight) {
      distTo[w.identifier] = distTo[v.identifier]!! + edge.weight
      edgeTo[w.identifier] = edge
    }
  }
}
