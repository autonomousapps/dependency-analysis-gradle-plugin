package com.autonomousapps.internal.graph

import com.autonomousapps.test.graphOf
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage") // Guava
internal class GraphsTest {

  @Nested inner class Topological {
    /** From Algorithms, 4th Ed, p583. */
    @Test fun `can compute topological order`() {
      // Given
      val graph = graphOf(
        0 to 5,
        0 to 1,
        0 to 6,
        5 to 4,
        2 to 0,
        2 to 3,
        6 to 4
      )

      // When
      val top = Topological(graph, 0)

      // Then
      assertThat(top.order).containsExactly(0, 6, 1, 5, 4).inOrder()
    }

    @Test fun `can compute topological order of androidx-core-core-1-1-0`() {
      // Given a real dependency graph
      val graph = graphOf(
        ":app" to ":lib",
        ":app" to "androidx.core:core:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.versionedparcelable:versionedparcelable:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.collection:collection:1.0.0",
        "androidx.core:core:1.1.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.lifecycle:lifecycle-runtime:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.lifecycle:lifecycle-common:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.arch.core:core-common:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.arch.core:core-common:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.lifecycle:lifecycle-common:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.collection:collection:1.0.0",
        "androidx.collection:collection:1.0.0" to "androidx.annotation:annotation:1.1.0",
      )

      // When
      val top = Topological(graph, ":app")

      // Then
      val expected = listOf(
        ":app",
        "androidx.core:core:1.1.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0",
        "androidx.arch.core:core-common:2.0.0",
        "androidx.lifecycle:lifecycle-common:2.0.0",
        "androidx.versionedparcelable:versionedparcelable:1.1.0",
        "androidx.collection:collection:1.0.0",
        "androidx.annotation:annotation:1.1.0",
        ":lib"
      )
      assertThat(top.order).containsExactlyElementsIn(expected).inOrder()
    }
  }

  @Nested inner class ShortestPath {
    @Test fun `can compute shortest path`() {
      // Given
      val graph = graphOf(
        0 to 1,
        1 to 2,
        2 to 3,
        0 to 4,
        4 to 3
      )

      // When
      val paths = ShortestPath(graph, 0)

      // Then
      assertThat(paths.hasPathTo(3)).isTrue()
      assertThat(paths.pathTo(3)).containsExactly(0, 4, 3).inOrder()
    }
  }

  @Nested inner class DominanceTree {
    @Test fun `can compute dominance tree`() {
      // Given a real dependency graph
      val graph = graphOf(
        ":app" to ":lib",
        ":app" to "androidx.core:core:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.versionedparcelable:versionedparcelable:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.collection:collection:1.0.0",
        "androidx.core:core:1.1.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.lifecycle:lifecycle-runtime:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.lifecycle:lifecycle-common:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.arch.core:core-common:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.arch.core:core-common:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.lifecycle:lifecycle-common:2.0.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.versionedparcelable:versionedparcelable:1.1.0" to "androidx.collection:collection:1.0.0",
        "androidx.collection:collection:1.0.0" to "androidx.annotation:annotation:1.1.0",
      )

      // When
      val tree = DominanceTree(graph, ":app")

      // Then
      val expected = graphOf(
        ":app" to ":lib",
        ":app" to "androidx.core:core:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.annotation:annotation:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.lifecycle:lifecycle-runtime:2.0.0",
        "androidx.core:core:1.1.0" to "androidx.versionedparcelable:versionedparcelable:1.1.0",
        "androidx.core:core:1.1.0" to "androidx.collection:collection:1.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.lifecycle:lifecycle-common:2.0.0",
        "androidx.lifecycle:lifecycle-runtime:2.0.0" to "androidx.arch.core:core-common:2.0.0",
      )
      assertThat(tree.dominanceGraph.edges()).containsExactlyElementsIn(expected.edges())
    }
  }
}
