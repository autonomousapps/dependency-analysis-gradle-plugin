// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class PeekJsonStringTest {

  @TempDir lateinit var tempDir: Path

  private fun jsonFile(name: String, content: String): File =
    tempDir.resolve(name).toFile().apply { writeText(content) }

  @Test fun `reads a top-level string property`() {
    val file = jsonFile("a.json", """{"projectPath":":lib","other":1}""")

    assertThat(file.peekJsonString("projectPath")).isEqualTo(":lib")
  }

  @Test fun `skips over earlier properties, including nested ones`() {
    val file = jsonFile(
      "b.json",
      """{"nested":{"projectPath":"wrong"},"list":[1,2,{"projectPath":"also wrong"}],"projectPath":":right"}"""
    )

    assertThat(file.peekJsonString("projectPath")).isEqualTo(":right")
  }

  @Test fun `returns null when the property is absent`() {
    val file = jsonFile("c.json", """{"other":1}""")

    assertThat(file.peekJsonString("projectPath")).isNull()
  }

  @Test fun `returns null when the property is not a string`() {
    val file = jsonFile("d.json", """{"projectPath":42}""")

    assertThat(file.peekJsonString("projectPath")).isNull()
  }

  @Test fun `returns null when the document is not an object`() {
    val file = jsonFile("e.json", """[{"projectPath":":lib"}]""")

    assertThat(file.peekJsonString("projectPath")).isNull()
  }

  @Test fun `finds the project path of a real serialized ProjectAdvice`() {
    val advice = ProjectAdvice(
      projectPath = ":features:home",
      dependencyAdvice = setOf(
        Advice.ofRemove(
          coordinates = ProjectCoordinates(":lib", GradleVariantIdentification.EMPTY),
          fromConfiguration = "implementation",
        )
      ),
    )
    val file = tempDir.resolve("advice.json").toFile().apply { bufferWriteJson(advice) }

    assertThat(file.peekJsonString("projectPath")).isEqualTo(":features:home")
  }
}
