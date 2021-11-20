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
        ProjectCoordinates(":app"), "fooRegistry"
      )
    )

    val serialized = linterDependency.toJson()
    assertThat(serialized).isEqualTo(
      """[{"coordinates":{"type":"project","identifier":":app"},"lintRegistry":"fooRegistry"}]"""
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
      """[{"coordinates":{"type":"module","identifier":"magic:app","resolvedVersion":"1.0"},"lintRegistry":"fooRegistry"}]"""
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
}
