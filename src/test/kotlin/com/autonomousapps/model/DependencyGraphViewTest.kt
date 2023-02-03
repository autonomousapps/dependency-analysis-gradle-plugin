// Guava Graph
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.model

import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.declaration.Variant
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DependencyGraphViewTest {

  @Test fun `can serialize and deserialize DependencyGraphViews`() {
    val graphView = DependencyGraphView(
      Variant.MAIN,
      "compileClasspath",
      DependencyGraphView.newGraphBuilder()
        .addNode(":secondary:root".toProject())
        .putEdge(":root".toProject(), "foo:bar:1".toModule())
        .putEdge("foo:bar:1".toModule(), "bar:baz:1".toModule())
        .build()
    )

    val serialized = graphView.toJson()
    assertThat(serialized).isEqualTo(
      """
        {"variant":{"variant":"main","kind":"MAIN"},"configurationName":"compileClasspath","nodes":[{"type":"project","identifier":":secondary:root","capability":"some.group:root"},{"type":"project","identifier":":root","capability":"some.group:root"},{"type":"module","identifier":"foo:bar","resolvedVersion":"1","capability":"foo:bar"},{"type":"module","identifier":"bar:baz","resolvedVersion":"1","capability":"bar:baz"}],"edges":[{"source":{"type":"project","identifier":":root","capability":"some.group:root"},"target":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","capability":"foo:bar"}},{"source":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","capability":"foo:bar"},"target":{"type":"module","identifier":"bar:baz","resolvedVersion":"1","capability":"bar:baz"}}]}
      """.trimIndent()
    )

    val deserialized = serialized.fromJson<DependencyGraphView>()
    assertThat(deserialized).isEqualTo(graphView)
  }

  private fun String.toProject() = ProjectCoordinates(this, "some.group${substring(lastIndexOf(":"))}")
  private fun String.toModule() = ModuleCoordinates(substringBeforeLast(':'), substringAfterLast(':'))
}
