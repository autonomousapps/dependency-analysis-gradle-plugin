// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.graph

import com.autonomousapps.graph.utils.GraphAdapter.StringGraphContainer
import com.autonomousapps.graph.utils.fromJson
import com.autonomousapps.graph.utils.toJson
import com.google.common.graph.Graph
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStream
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

internal class DominanceTreeTest {

  @Test fun `can build dominance tree`() {
    // Given
    val json = textFromResource("graph-runtime.json")
    val graph = json.fromJson<StringGraphContainer>().graph

    // When
    val tree = DominanceTree(graph)
    // writeGoldenTree(tree) // use when re-generating the golden tree

    // Then
    assertThat(tree.selfDominatingNodes()).containsExactly(tree.root)
    assertThat(tree.dominanceGraph).isEqualTo(goldenTree())
  }

  @Suppress("unused")
  private fun writeGoldenTree(tree: DominanceTree<String>) {
    val output = Paths.get(".").normalize().resolve("golden-tree.json")
    println("output = ${output.absolutePathString()}")
    output.writeText(StringGraphContainer(tree.dominanceGraph).toJson())
  }

  @Suppress("UnstableApiUsage")
  private fun goldenTree(): Graph<String> {
    val json = textFromResource("golden-tree.json")
    return json.fromJson<StringGraphContainer>().graph
  }
}

private fun Any.textFromResource(resourcePath: String): String {
  return streamFromResource(resourcePath).bufferedReader().use(BufferedReader::readText)
}

private fun Any.streamFromResource(resourcePath: String): InputStream {
  return javaClass.classLoader.getResourceAsStream(resourcePath) ?: error("No resource at '$resourcePath'")
}
