// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.model.Advice
import com.autonomousapps.model.BuildHealth
import com.autonomousapps.model.BuildHealth.AndroidScoreMetrics
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class BuildHealthWriterTest {

  @TempDir lateinit var tempDir: Path

  private fun coordinates(path: String) = ProjectCoordinates(path, GradleVariantIdentification.EMPTY)

  private fun adviceFor(projectPath: String, shouldFail: Boolean = false) = ProjectAdvice(
    projectPath = projectPath,
    dependencyAdvice = setOf(
      Advice.ofRemove(coordinates("$projectPath:unused"), "implementation"),
      Advice.ofAdd(coordinates("$projectPath:missing"), "implementation"),
      Advice.ofChange(coordinates("$projectPath:wrong"), "api", "implementation"),
    ),
    shouldFail = shouldFail,
  )

  @Test fun `streamed output is byte-identical to serializing an assembled BuildHealth`() {
    val advice = listOf(
      adviceFor(":app", shouldFail = true),
      adviceFor(":lib"),
      adviceFor(":feature:home"),
    ).sorted()

    val expectedFile = tempDir.resolve("expected.json").toFile()
    expectedFile.bufferWriteJson(
      BuildHealth(
        projectAdvice = advice.toSortedSet(),
        shouldFail = true,
        projectCount = advice.size,
        unusedCount = 3,
        undeclaredCount = 3,
        misDeclaredCount = 3,
        compileOnlyCount = 0,
        runtimeOnlyCount = 0,
        processorCount = 0,
        androidScoreMetrics = AndroidScoreMetrics(shouldBeJvmCount = 0, couldBeJvmCount = 0),
      )
    )

    val actualFile = tempDir.resolve("actual.json").toFile()
    val shouldFail = BuildHealthWriter(actualFile).write(advice.asSequence(), projectCount = advice.size)

    assertThat(shouldFail).isTrue()
    assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())
  }

  @Test fun `the streamed document round-trips back into BuildHealth`() {
    val advice = listOf(adviceFor(":app"), adviceFor(":lib")).sorted()

    val file = tempDir.resolve("health.json").toFile()
    BuildHealthWriter(file).write(advice.asSequence(), projectCount = advice.size)

    val buildHealth = file.fromJson<BuildHealth>()

    assertThat(buildHealth.projectCount).isEqualTo(2)
    assertThat(buildHealth.unusedCount).isEqualTo(2)
    assertThat(buildHealth.undeclaredCount).isEqualTo(2)
    assertThat(buildHealth.misDeclaredCount).isEqualTo(2)
    assertThat(buildHealth.shouldFail).isFalse()
    assertThat(buildHealth.projectAdvice.map { it.projectPath }).containsExactly(":app", ":lib")
  }

  @Test fun `empty project advice is written but not counted or reported`() {
    val advice = listOf(ProjectAdvice(projectPath = ":empty"), adviceFor(":lib")).sorted()

    val reported = mutableListOf<String>()
    val file = tempDir.resolve("health.json").toFile()
    BuildHealthWriter(file).write(advice.asSequence(), projectCount = advice.size) {
      reported += it.projectPath
    }

    val buildHealth = file.fromJson<BuildHealth>()

    assertThat(buildHealth.projectAdvice.map { it.projectPath }).containsExactly(":empty", ":lib")
    assertThat(buildHealth.projectCount).isEqualTo(2)
    assertThat(reported).containsExactly(":lib")
    assertThat(buildHealth.unusedCount).isEqualTo(1)
  }

  @Test fun `an empty build produces a valid document`() {
    val file = tempDir.resolve("health.json").toFile()
    val shouldFail = BuildHealthWriter(file).write(emptySequence(), projectCount = 0)

    val buildHealth = file.fromJson<BuildHealth>()

    assertThat(shouldFail).isFalse()
    assertThat(buildHealth.projectAdvice).isEmpty()
    assertThat(buildHealth.projectCount).isEqualTo(0)
  }

  @Test fun `the sequence is consumed lazily, one element at a time`() {

    var live = 0
    var maxLive = 0

    val advice = generateSequence(0) { it + 1 }
      .take(50)
      .map {
        live++
        maxLive = maxOf(maxLive, live)
        adviceFor(":project$it")
      }
      .map { it.also { live-- } }

    val file = tempDir.resolve("health.json").toFile()
    BuildHealthWriter(file).write(advice, projectCount = 50)

    assertThat(maxLive).isEqualTo(1)
    assertThat(file.fromJson<BuildHealth>().projectAdvice).hasSize(50)
  }
}
