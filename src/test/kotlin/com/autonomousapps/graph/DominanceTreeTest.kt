package com.autonomousapps.graph

import com.autonomousapps.internal.utils.GraphAdapter.StringGraphContainer
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.test.textFromResource
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

// TODO(tsr): move to graph-support module?
internal class DominanceTreeTest {

  @Test fun `can build dominance tree`() {
    // Given
    val json = textFromResource("graph-runtime.json")
    val graph = json.fromJson<StringGraphContainer>().graph

    // When
    val tree = DominanceTree(graph, ":root")

    // Then
    assertThat(true).isFalse()
  }
}
