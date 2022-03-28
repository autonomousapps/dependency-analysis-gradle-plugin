package com.autonomousapps.graph

import com.google.common.graph.ElementOrder
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage") // Guava
internal class GraphsTest {

  /** From Algorithms, 4th Ed, p583. */
  @Test fun `can compute topological order`() {
    // Given
    val graph = graphOf(
      0, 5,
      0, 1,
      0, 6,
      5, 4,
      2, 0,
      2, 3,
      6, 4
    )

    // When
    val top = Graphs.Topological(graph, 0)

    // Then
    assertThat(top.order).containsExactly(0, 6, 1, 5, 4).inOrder()
  }

  @Test fun `can compute shortest path`() {
    // Given
    val graph = graphOf(
      0, 1,
      1, 2,
      2, 3,
      0, 4,
      4, 3
    )

    // When
    val paths = Graphs.ShortestPath(graph, 0)

    // Then
    assertThat(paths.hasPathTo(3)).isTrue()
    assertThat(paths.pathTo(3)).containsExactly(0, 4, 3).inOrder()
  }

  private fun <N : Any> graphOf(vararg elements: N): ImmutableGraph<N> {
    val builder: ImmutableGraph.Builder<N> = GraphBuilder.directed()
      .allowsSelfLoops(false)
      .incidentEdgeOrder(ElementOrder.stable<N>())
      .immutable()

    elements.toList().chunked(2).forEach {
      if (it.size != 2) throw IllegalArgumentException("Expected elements % 2 == 0")
      builder.putEdge(it[0], it[1])
    }
    return builder.build()
  }
}
