// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.PublicTypes
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class TransitiveExposureIndexTest {

  private fun usage(
    projectPath: String,
    projectDependencies: Map<String, Set<String>> = emptyMap(),
    libraryDependencies: Map<String, Set<String>> = emptyMap(),
  ) = AggregateTypeUsageReport(
    projectPath = projectPath,
    internal = emptySet(),
    projectDependencies = projectDependencies,
    libraryDependencies = libraryDependencies,
  )

  private fun publishes(projectPath: String, vararg types: String) =
    PublicTypes(projectPath = projectPath, types = types.toSet())

  private fun exposes(projectPath: String, vararg types: String) =
    PublicTypes(projectPath = projectPath, types = emptySet(), exposedTypes = types.toSet())

  @Test fun `a dependency used by a direct consumer is exposed`() {
    // :consumer -> :middle -> :producer, and :consumer uses a type from :producer.
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":consumer", projectDependencies = mapOf(":middle" to setOf("com.example.producer.Widget"))),
        usage(":middle", projectDependencies = mapOf(":producer" to emptySet())),
      ),
      publicTypes = listOf(publishes(":producer", "com.example.producer.Widget")),
      maxDepth = 1,
    )

    assertThat(index.isTransitivelyExposed(":middle", ":producer")).isTrue()
  }

  @Test fun `a dependency nobody downstream uses is not exposed`() {
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":consumer", projectDependencies = mapOf(":middle" to setOf("com.example.middle.Middle"))),
        usage(":middle", projectDependencies = mapOf(":producer" to emptySet())),
      ),
      publicTypes = listOf(publishes(":producer", "com.example.producer.Widget")),
      maxDepth = 1,
    )

    assertThat(index.isTransitivelyExposed(":middle", ":producer")).isFalse()
  }

  @Test fun `a dependency that publishes nothing is never exposed`() {
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":consumer", projectDependencies = mapOf(":middle" to setOf("com.example.producer.Widget"))),
      ),
      publicTypes = listOf(publishes(":producer")),
      maxDepth = 1,
    )

    assertThat(index.isTransitivelyExposed(":middle", ":producer")).isFalse()
  }

  @Test fun `a two-hop chain is invisible at the default depth`() {
    // :far -> :near -> :middle -> :producer. Only :far uses the exposed type.
    val typeUsages = listOf(
      usage(":far", projectDependencies = mapOf(":near" to setOf("com.example.producer.Widget"))),
      usage(":near", projectDependencies = mapOf(":middle" to emptySet())),
      usage(":middle", projectDependencies = mapOf(":producer" to emptySet())),
    )
    val publicTypes = listOf(publishes(":producer", "com.example.producer.Widget"))

    val shallow = TransitiveExposureIndex.of(typeUsages, publicTypes, maxDepth = 1)
    assertThat(shallow.isTransitivelyExposed(":middle", ":producer")).isFalse()

    val deep = TransitiveExposureIndex.of(typeUsages, publicTypes, maxDepth = 2)
    assertThat(deep.isTransitivelyExposed(":middle", ":producer")).isTrue()
  }

  @Test fun `types attributed to a library dependency still count`() {
    // The consumer resolved the type through a library coordinate rather than a project one.
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(
          ":consumer",
          projectDependencies = mapOf(":middle" to emptySet()),
          libraryDependencies = mapOf("com.example:producer" to setOf("com.example.producer.Widget")),
        ),
      ),
      publicTypes = listOf(publishes(":producer", "com.example.producer.Widget")),
      maxDepth = 1,
    )

    assertThat(index.isTransitivelyExposed(":middle", ":producer")).isTrue()
  }

  @Test fun `a dependency cycle terminates`() {
    // :a and :b consume each other. The search must not loop.
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":a", projectDependencies = mapOf(":b" to emptySet())),
        usage(":b", projectDependencies = mapOf(":a" to emptySet())),
      ),
      publicTypes = listOf(publishes(":producer", "com.example.producer.Widget")),
      maxDepth = 10,
    )

    assertThat(index.isTransitivelyExposed(":a", ":producer")).isFalse()
  }

  @Test fun `a depth below one is treated as one`() {
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":consumer", projectDependencies = mapOf(":middle" to setOf("com.example.producer.Widget"))),
      ),
      publicTypes = listOf(publishes(":producer", "com.example.producer.Widget")),
      maxDepth = 0,
    )

    assertThat(index.isTransitivelyExposed(":middle", ":producer")).isTrue()
  }

  @Test fun `a dependency reached through another dependency's ABI is exposed`() {
    // :app uses :middle. :middle's ABI leaks Widget, which :producer publishes. :app therefore compiles
    // against :producer's types without any consumer of :app existing.
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":app", projectDependencies = mapOf(":middle" to setOf("com.example.middle.Middle"))),
      ),
      publicTypes = listOf(
        publishes(":producer", "com.example.producer.Widget"),
        exposes(":middle", "com.example.producer.Widget"),
      ),
      maxDepth = 1,
    )

    assertThat(index.isExposedThroughDependencyAbi(":app", ":producer")).isTrue()
    // Nobody consumes :app, so the downstream check alone would have missed it.
    assertThat(index.isTransitivelyExposed(":app", ":producer")).isFalse()
  }

  @Test fun `a dependency no other dependency leaks is not ABI-exposed`() {
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":app", projectDependencies = mapOf(":middle" to setOf("com.example.middle.Middle"))),
      ),
      publicTypes = listOf(
        publishes(":producer", "com.example.producer.Widget"),
        exposes(":middle", "com.example.other.Thing"),
      ),
      maxDepth = 1,
    )

    assertThat(index.isExposedThroughDependencyAbi(":app", ":producer")).isFalse()
  }

  @Test fun `a dependency does not expose itself`() {
    // :middle publishing and exposing the same type must not make "remove :middle from :app" look like
    // a false positive.
    val index = TransitiveExposureIndex.of(
      typeUsages = listOf(
        usage(":app", projectDependencies = mapOf(":middle" to emptySet())),
      ),
      publicTypes = listOf(
        PublicTypes(":middle", types = setOf("com.example.middle.Middle"), exposedTypes = setOf("com.example.middle.Middle")),
      ),
      maxDepth = 1,
    )

    assertThat(index.isExposedThroughDependencyAbi(":app", ":middle")).isFalse()
  }
}
