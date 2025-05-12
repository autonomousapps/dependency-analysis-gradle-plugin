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
import com.autonomousapps.model.source.JvmSourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DependencyGraphViewTest {

  @Test fun `can serialize and deserialize DependencyGraphViews`() {
    val graphView = DependencyGraphView(
      JvmSourceKind.MAIN,
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
        {"sourceKind":{"type":"jvm","name":"main","kind":"MAIN","compileClasspathName":"compileClasspath","runtimeClasspathName":"runtimeClasspath"},"configurationName":"compileClasspath","graphJson":{"nodes":[{"type":"module","identifier":"bar:baz","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"project","identifier":":root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},{"type":"project","identifier":":secondary:root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}],"edges":[{"source":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},"target":{"type":"module","identifier":"bar:baz","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}},{"source":{"type":"project","identifier":":root","gradleVariantIdentification":{"capabilities":[],"attributes":{}}},"target":{"type":"module","identifier":"foo:bar","resolvedVersion":"1","gradleVariantIdentification":{"capabilities":[],"attributes":{}}}}]}}
      """.trimIndent()
    )

    val deserialized = serialized.fromJson<DependencyGraphView>()
    assertThat(deserialized).isEqualTo(graphView)
  }

  private fun String.toProject() = ProjectCoordinates(this, GradleVariantIdentification.EMPTY)
  private fun String.toModule() =
    ModuleCoordinates(substringBeforeLast(':'), substringAfterLast(':'), GradleVariantIdentification.EMPTY)
}
