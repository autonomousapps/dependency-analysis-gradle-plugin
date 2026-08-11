// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

internal class ArtifactsExpanderTest {

  @TempDir lateinit var tempDir: Path

  @Test fun `finds a single jar file`() {
    val jarFile = tempDir.resolve("foo.jar").createFile().toFile()

    val files = ArtifactsExpander.maybeExpand(jarFile)

    assertThat(files).hasSize(1)
  }

  @Test fun `finds a single directory`() {
    val javaMain = tempDir.resolve("foo/bar/build/classes/java/main").createDirectories().toFile()

    val files = ArtifactsExpander.maybeExpand(javaMain)

    assertThat(files).hasSize(1)
  }

  @Test fun `finds two directories`() {
    val javaMain = tempDir.resolve("foo/bar/build/classes/java/main").createDirectories().toFile()
    tempDir.resolve("foo/bar/build/classes/kotlin/main").createDirectories().toFile()

    val files = ArtifactsExpander.maybeExpand(javaMain)

    assertThat(files).hasSize(2)
  }

  @Test fun `does not find directory in different source set`() {
    val javaMain = tempDir.resolve("foo/bar/build/classes/java/main").createDirectories().toFile()
    tempDir.resolve("foo/bar/build/classes/java/test").createDirectories().toFile()

    val files = ArtifactsExpander.maybeExpand(javaMain)

    assertThat(files).hasSize(1)
  }

  @Test fun `does not find directory in different source set and different language`() {
    val javaMain = tempDir.resolve("foo/bar/build/classes/java/main").createDirectories().toFile()
    tempDir.resolve("foo/bar/build/classes/kotlin/test").createDirectories().toFile()

    val files = ArtifactsExpander.maybeExpand(javaMain)

    assertThat(files).hasSize(1)
  }

  @Test fun `finds correct directory sets`() {
    // main
    val javaMain = tempDir.resolve("foo/bar/build/classes/java/main").createDirectories().toFile()

    // testFixtures
    val javaTestFixtures = tempDir.resolve("foo/bar/build/classes/java/testFixtures").createDirectories().toFile()
    tempDir.resolve("foo/bar/build/classes/kotlin/testFixtures").createDirectories().toFile()

    assertThat(ArtifactsExpander.maybeExpand(javaMain)).hasSize(1)
    assertThat(ArtifactsExpander.maybeExpand(javaTestFixtures)).hasSize(2)
  }
}
