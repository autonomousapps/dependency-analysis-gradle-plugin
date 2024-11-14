// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.internal.intermediates.AndroidLinterDependency
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CoordinatesTest {

  private val gvi = GradleVariantIdentification(setOf("some:capability"), mapOf("someAttribute" to "blue"))

  @Test fun `can serialize and deserialize polymorphic ProjectCoordinates with moshi`() {
    val linterDependency = setOf(
      AndroidLinterDependency(
        ProjectCoordinates(":app", gvi), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"project","identifier":":app","gradleVariantIdentification":{"capabilities":["some:capability"],"attributes":{"someAttribute":"blue"}}},"lintRegistry":"fooRegistry"}]"""
    )

    val deserialized = serialized.fromJsonSet<AndroidLinterDependency>()
    assertThat(deserialized).isEqualTo(linterDependency)
  }

  @Test fun `can serialize and deserialize polymorphic ModuleCoordinates with moshi`() {
    val linterDependency = setOf(
      AndroidLinterDependency(
        ModuleCoordinates("magic:app", "1.0", gvi), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"module","identifier":"magic:app","resolvedVersion":"1.0","gradleVariantIdentification":{"capabilities":["some:capability"],"attributes":{"someAttribute":"blue"}}},"lintRegistry":"fooRegistry"}]"""
    )

    val deserialized = serialized.fromJsonSet<AndroidLinterDependency>()
    assertThat(deserialized).isEqualTo(linterDependency)
  }

  @Test fun `can serialize and deserialize polymorphic FlatCoordinates with moshi`() {
    val linterDependency = setOf(
      AndroidLinterDependency(
        FlatCoordinates("Gradle API"), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"flat","identifier":"Gradle API"},"lintRegistry":"fooRegistry"}]"""
    )

    val deserialized = serialized.fromJsonSet<AndroidLinterDependency>()
    assertThat(deserialized).isEqualTo(linterDependency)
  }

  @Test fun `compares to behaves similarly in both directions`() {
    val moduleA = ModuleCoordinates("g:a", "1.0", gvi)
    val moduleB = ModuleCoordinates("g:b", "1.0", gvi)
    val includedB = IncludedBuildCoordinates("g:a", ProjectCoordinates(":a", gvi), gvi)
    val includedA = IncludedBuildCoordinates("g:b", ProjectCoordinates(":b", gvi), gvi)
    val projectA = ProjectCoordinates(":a", gvi)
    val projectB = ProjectCoordinates(":b", gvi)
    val flatA = FlatCoordinates("a")
    val flatB = FlatCoordinates("a")

    assertThat(moduleA.compareTo(moduleB)).isEqualTo(moduleB.compareTo(moduleA) * -1)
    assertThat(moduleA.compareTo(includedB)).isEqualTo(includedB.compareTo(moduleA) * -1)
    assertThat(moduleA.compareTo(projectB)).isEqualTo(projectB.compareTo(moduleA) * -1)
    assertThat(moduleA.compareTo(flatB)).isEqualTo(flatB.compareTo(moduleA) * -1)

    assertThat(includedA.compareTo(moduleB)).isEqualTo(moduleB.compareTo(includedA) * -1)
    assertThat(includedA.compareTo(includedB)).isEqualTo(includedB.compareTo(includedA) * -1)
    assertThat(includedA.compareTo(projectB)).isEqualTo(projectB.compareTo(includedA) * -1)
    assertThat(includedA.compareTo(flatB)).isEqualTo(flatB.compareTo(includedA) * -1)

    assertThat(projectA.compareTo(moduleB)).isEqualTo(moduleB.compareTo(projectA) * -1)
    assertThat(projectA.compareTo(includedB)).isEqualTo(includedB.compareTo(projectA) * -1)
    assertThat(projectA.compareTo(projectB)).isEqualTo(projectB.compareTo(projectA) * -1)
    assertThat(projectA.compareTo(flatB)).isEqualTo(flatB.compareTo(projectA) * -1)

    assertThat(flatA.compareTo(moduleB)).isEqualTo(moduleB.compareTo(flatA) * -1)
    assertThat(flatA.compareTo(includedB)).isEqualTo(includedB.compareTo(flatA) * -1)
    assertThat(flatA.compareTo(projectB)).isEqualTo(projectB.compareTo(flatA) * -1)
    assertThat(flatA.compareTo(flatB)).isEqualTo(flatB.compareTo(flatA) * -1)
  }

  /**
   * Documentation for future-me as to why `appcompat-resources:` is sorted earlier than `appcompat:`. This relates to
   * recent changes in [Coordinates.compareTo] and how [ModuleCoordinates] are sorted against one another.
   */
  @Test fun `hyphens are smaller than colons`() {
    val dependencies = listOf(
      "androidx.activity:activity:1.0.0",
      "androidx.annotation:annotation:1.1.0",
      "androidx.appcompat:appcompat-resources:1.1.0",
      "androidx.appcompat:appcompat:1.1.0"
    )

    assertThat(dependencies)
      .containsExactlyElementsIn(dependencies.sorted())
      .inOrder()

    assertThat('-'.code).isLessThan(':'.code)
  }
}
