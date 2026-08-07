// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class ArtifactsReportTaskTest {

  @Test fun `mixed JVM outputs remain scoped to the resolved source set`(@TempDir dir: File) {
    val classesDirectory = dir.resolve("build/classes")
    val javaMain = classesDirectory.resolve("java/main")
    val kotlinMain = classesDirectory.resolve("kotlin/main")
    val javaTestFixtures = classesDirectory.resolve("java/testFixtures")
    val kotlinTestFixtures = classesDirectory.resolve("kotlin/testFixtures")

    assertThat(kotlinMain.sourceSetClassDirectories()).containsExactly(javaMain, kotlinMain)
    assertThat(javaTestFixtures.sourceSetClassDirectories()).containsExactly(javaTestFixtures, kotlinTestFixtures)
  }

  @Test fun `nonstandard class directory remains unchanged`(@TempDir dir: File) {
    val classDirectory = dir.resolve("custom/classes")

    assertThat(classDirectory.sourceSetClassDirectories()).containsExactly(classDirectory)
  }
}
