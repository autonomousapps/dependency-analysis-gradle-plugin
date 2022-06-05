package com.autonomousapps.internal.graph

import com.google.common.graph.Graph
import com.google.common.graph.Traverser
import java.util.*

/** With thanks to Algorithms, 4th Ed. See p582 for the explanation for why we want the reverse postorder. */
@Suppress("UnstableApiUsage") // Guava graphs
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
