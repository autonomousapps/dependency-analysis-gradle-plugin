package com.autonomousapps.graph

import com.autonomousapps.internal.utils.GraphAdapter.StringGraphContainer
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.test.textFromResource
import com.google.common.graph.Graph
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

// TODO(tsr): move to graph-support module?
// TODO(tsr): move moshi string-graph stuff to test-only module?
internal class DominanceTreeTest {

  @Test fun `can build dominance tree`() {
    // Given
    val json = textFromResource("graph-runtime.json")
    val graph = json.fromJson<StringGraphContainer>().graph

    // When
    val tree = DominanceTree(graph)

    // Then the only
    assertThat(tree.selfDominatingNodes()).containsExactly(tree.root)
    assertThat(tree.dominanceGraph).isEqualTo(goldenTree())
  }

  private fun goldenTree(): Graph<String> {
    val json = textFromResource("golden-tree.json")
    return json.fromJson<StringGraphContainer>().graph
  }
}
