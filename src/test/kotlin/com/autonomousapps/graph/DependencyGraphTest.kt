package com.autonomousapps.graph

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DependencyGraphTest {

  @Test fun test() {
    val node1 = ConsumerNode(":proj")
    val node2 = ProducerNode("junit")
    val node3 = ProducerNode("truth")

    val graph = DependencyGraph()
    graph.addEdge(Edge(node1, node2))
    graph.addEdge(Edge(node1, node3))
    graph.addEdge(Edge(node2, node3))

    println("GRAPH\n$graph")
    assertThat(true).isFalse() // TODO delete or make this a meaningful test
  }
}