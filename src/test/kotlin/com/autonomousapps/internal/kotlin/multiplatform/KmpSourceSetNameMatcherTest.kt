// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.internal.kmp.SourceGraph
import com.autonomousapps.model.source.KmpSourceKind
import com.google.common.graph.Graph
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class KmpSourceSetNameMatcherTest {

  private val defaultMap = mapOf(
    "androidMain" to setOf("commonJvmMain"),
    "commonJvmMain" to setOf("commonMain"),
    "commonJvmTest" to setOf("commonTest"),
    "commonMain" to emptySet(),
    "commonTest" to emptySet(),
    "jvmMain" to setOf("commonJvmMain"),
    "jvmTest" to setOf("commonJvmTest"),
  )

  private fun sourceSetGraph(map: Map<String, Set<String>> = defaultMap): Graph<String> = SourceGraph(map).graph

  // This is the KmpSourceKind being analyzed (the target / compilation)
  private val compilation = KmpSourceKind.of("jvmMain")
  private val matcher = KmpSourceSetNameMatcher(sourceSetGraph(), compilation)

  @Test fun `commonJvmMain is the LCA 1`() {
    // Given
    val classes = setOf(
      ClassReference("Jvm", "jvmMain"),
      ClassReference("Android", "androidMain"),
      ClassReference("CommonJvm", "commonJvmMain"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmMain"))
  }

  @Test fun `commonJvmMain is the LCA 2`() {
    // Given
    val classes = setOf(
      ClassReference("Jvm", "jvmMain"),
      ClassReference("Android", "androidMain"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmMain"))
  }

  @Test fun `a class ref to commonMain is commonMain`() {
    // Given
    val classes = setOf(ClassReference("foo", "commonMain"))

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonMain"))
  }

  @Test fun `a class ref to commonMain and commonTest is commonMain`() {
    // Given
    val classes = setOf(
      ClassReference("foo", "commonTest"),
      ClassReference("bar", "commonMain"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonMain"))
  }

  @Test fun `a class ref to androidMain and commonJvmMain is commonJvmMain`() {
    // Given
    val classes = setOf(
      ClassReference("foo", "androidMain"),
      ClassReference("bar", "commonJvmMain"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmMain"))
  }


  @Test fun `a class ref to jvmTest and androidMain is commonMain`() {
    // Given
    val classes = setOf(
      ClassReference("foo", "jvmTest"),
      ClassReference("bar", "androidMain"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonMain"))
  }

  @Test fun `a class ref to jvmTest and commonJvmTest is commonJvmTest`() {
    // Given
    val classes = setOf(
      ClassReference("foo", "jvmTest"),
      ClassReference("foo", "commonJvmTest"),
    )

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmTest"))
  }

  @Test fun `a class ref to commonTest is commonTest`() {
    // Given
    val classes = setOf(ClassReference("foo", "commonTest"))

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonTest"))
  }

  @Test fun `a class ref to commonJvmMain is commonJvmMain`() {
    // Given
    val classes = setOf(ClassReference("foo", "commonJvmMain"))

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmMain"))
  }

  @Test fun `a class ref to commonJvmTest is commonJvmTest`() {
    // Given
    val classes = setOf(ClassReference("foo", "commonJvmTest"))

    // When
    val rootMost = matcher.getLowestCommonAncestor(classes)

    // Then
    assertThat(rootMost).isEqualTo(compilation.copy(name = "commonJvmTest"))
  }
}
