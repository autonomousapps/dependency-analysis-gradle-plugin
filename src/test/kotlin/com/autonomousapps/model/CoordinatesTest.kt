package com.autonomousapps.model

import com.autonomousapps.internal.utils.fromJsonSet
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.intermediates.AndroidLinterDependency
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CoordinatesTest {

  @Test fun `can serialize and deserialize polymorphic ProjectCoordinates with moshi`() {
    val linterDependency = setOf(
      AndroidLinterDependency(
        ProjectCoordinates(":app", "some.group:app"), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"project","identifier":":app","capability":"some.group:app"},"lintRegistry":"fooRegistry"}]"""
    )

    val deserialized = serialized.fromJsonSet<AndroidLinterDependency>()
    assertThat(deserialized).isEqualTo(linterDependency)
  }

  @Test fun `can serialize and deserialize polymorphic ModuleCoordinates with moshi`() {
    val linterDependency = setOf(
      AndroidLinterDependency(
        ModuleCoordinates("magic:app", "1.0"), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"module","identifier":"magic:app","resolvedVersion":"1.0","capability":"magic:app"},"lintRegistry":"fooRegistry"}]"""
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

  @Test fun `compares to behaves similar in both directions`() {
    val moduleA = ModuleCoordinates("g:a", "1.0")
    val moduleB = ModuleCoordinates("g:b", "1.0")
    val includedB = IncludedBuildCoordinates("g:a", ProjectCoordinates(":a", "some.group:a"), "some.group:a")
    val includedA = IncludedBuildCoordinates("g:b", ProjectCoordinates(":b", "some.group:b"), "some.group:b")
    val projectA = ProjectCoordinates(":a", "some.group:a")
    val projectB = ProjectCoordinates(":b", "some.group:b")
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
}
