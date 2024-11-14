// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
// Guava Graph
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.model.internal

import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
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
        {"variant":{"variant":"main","kind":"MAIN"},"configurationName":"compileClasspath","nodes":[{"type":"project","identifier":":secondary:root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"project","identifier":":root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"module","identifier":"bar:baz","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}],"edges":[{"source":{"type":"project","identifier":":root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},"target":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}},{"source":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},"target":{"type":"module","identifier":"bar:baz","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}}]}
      """.trimIndent()
    )

    val deserialized = serialized.fromJson<DependencyGraphView>()
    assertThat(deserialized).isEqualTo(graphView)
  }

  private fun String.toProject() = ProjectCoordinates(this, GradleVariantIdentification.EMPTY)
  private fun String.toModule() = ModuleCoordinates(substringBeforeLast(':'), substringAfterLast(':'), GradleVariantIdentification.EMPTY)
}
