// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.source.JvmSourceKind
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

@Suppress("UnstableApiUsage")
internal class RuntimeOnlyFilterTest {

  private val testGraph = TestGraph()
  private val simplifier = RuntimeOnlyFilter(testGraph.dependencyGraph, ":")

  @Test
  fun `keeps runtimeOnly advice when direct node is being removed`() {
    // Given
    val addUber = Advice.ofAdd(testGraph.conscrypt, "runtimeOnly")
    val removeDirect = Advice.ofRemove(testGraph.direct, "implementation")
    val advice = sequence {
      yield(addUber)
      yield(removeDirect)
    }

    // When
    val simplified = simplifier.simplify(advice)

    // Then
    Truth.assertThat(simplified.toList()).containsExactly(addUber, removeDirect)
  }

  @Test
  fun `drops runtimeOnly advice when direct node is being retained`() {
    // Given
    val addUber = Advice.ofAdd(testGraph.conscrypt, "runtimeOnly")
    val changeDirect = Advice.ofChange(testGraph.direct, "implementation", "api")
    val advice = sequence {
      yield(addUber)
      yield(changeDirect)
    }

    // When
    val simplified = simplifier.simplify(advice)

    // Then
    Truth.assertThat(simplified.toList()).containsExactly(changeDirect)
  }

  @Test
  fun `drops runtimeOnly advice when removed direct node is replaced by transitive provider`() {
    // Given
    val addUber = Advice.ofAdd(testGraph.conscrypt, "runtimeOnly")
    val addMarker = Advice.ofAdd(testGraph.pluginMarker, "api")
    val removeDirect = Advice.ofRemove(testGraph.direct, "implementation")
    val advice = sequence {
      yield(addUber)
      yield(addMarker)
      yield(removeDirect)
    }

    // When
    val simplified = simplifier.simplify(advice)

    // Then
    Truth.assertThat(simplified.toList()).containsExactly(addMarker, removeDirect)
  }

  @Test
  fun `keeps runtimeOnly advice when replacement is not visible to runtime source set`() {
    // Given
    val addUber = Advice.ofAdd(testGraph.conscrypt, "runtimeOnly")
    val addMarker = Advice.ofAdd(testGraph.pluginMarker, "testImplementation")
    val removeDirect = Advice.ofRemove(testGraph.direct, "implementation")
    val advice = sequence {
      yield(addUber)
      yield(addMarker)
      yield(removeDirect)
    }

    // When
    val simplified = simplifier.simplify(advice)

    // Then
    Truth.assertThat(simplified.toList()).containsExactly(addUber, addMarker, removeDirect)
  }

  private class TestGraph {
    val root = ProjectCoordinates(":root", GradleVariantIdentification.EMPTY)
    val direct = ProjectCoordinates(":direct", GradleVariantIdentification.EMPTY)
    val pluginMarker =
      ModuleCoordinates("com.example:com.example.gradle.plugin", "1.0", GradleVariantIdentification.EMPTY)
    val plugin = ModuleCoordinates("com.example:plugin", "1.0", GradleVariantIdentification.EMPTY)
    val conscrypt =
      ModuleCoordinates("org.conscrypt:conscrypt-openjdk-uber", "2.4.0", GradleVariantIdentification.EMPTY)

    private val graph = DependencyGraphView.newGraphBuilder().apply {
      addNode(root)
      addNode(direct)
      addNode(pluginMarker)
      addNode(plugin)
      addNode(conscrypt)

      putEdge(root, direct)
      putEdge(direct, conscrypt)
      putEdge(direct, pluginMarker)
      putEdge(pluginMarker, plugin)
      putEdge(plugin, conscrypt)
    }.build()

    private val graphView1 = DependencyGraphView(JvmSourceKind.of("main"), "compileClasspath", graph)
    private val graphView2 = DependencyGraphView(JvmSourceKind.of("main"), "runtimeClasspath", graph)
    private val graphView3 = DependencyGraphView(JvmSourceKind.of("test"), "testCompileClasspath", graph)
    private val graphView4 = DependencyGraphView(JvmSourceKind.of("test"), "testRuntimeClasspath", graph)

    // This replicates what DependencyGraphView.asMap() does. That function is internal and not exposed here, sadly.
    val dependencyGraph = mapOf(
      "main,MAIN,compileClasspath" to graphView1,
      "main,MAIN,runtimeClasspath" to graphView2,
      "test,TEST,testCompileClasspath" to graphView3,
      "test,TEST,testRuntimeClasspath" to graphView4,
    )
  }
}
