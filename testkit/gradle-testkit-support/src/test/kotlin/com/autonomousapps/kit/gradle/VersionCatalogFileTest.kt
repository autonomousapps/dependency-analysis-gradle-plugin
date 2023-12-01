package com.autonomousapps.kit.gradle

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class VersionCatalogFileTest {

  @Test fun `toString() works with basic constructor`() {
    // Given
    val catalog = VersionCatalogFile(
      """
        [versions]
        turtles = "1.0"
        
        [libraries]
        turtles = { module = "ankh:morpork", version.ref = "turtles" }
      """.trimIndent()
    )

    // Expect
    assertThat(catalog.toString()).isEqualTo(
      """
        [versions]
        turtles = "1.0"
        
        [libraries]
        turtles = { module = "ankh:morpork", version.ref = "turtles" }
      """.trimIndent()
    )
  }

  @Test fun `builder produces valid result`() {
    // Given
    val catalog = VersionCatalogFile.Builder().run {
      versions += listOf("""turtles = "1.0"""", """robots = "99"""")
      libraries += listOf(
        """turtles = { module = "ankh:morpork", version.ref = "turtles" }""",
        """robots = { module = "heart:of-gold", version.ref = "robots" }""",
      )
      build()
    }

    // Expect
    assertThat(catalog.toString()).isEqualTo(
      """
        [versions]
        turtles = "1.0"
        robots = "99"
        
        [libraries]
        turtles = { module = "ankh:morpork", version.ref = "turtles" }
        robots = { module = "heart:of-gold", version.ref = "robots" }
        
      """.trimIndent()
    )
  }
}
