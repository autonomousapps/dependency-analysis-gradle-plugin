// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.utils.bufferWriteJson
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.source.JvmSourceKind
import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

internal class ProjectVariantTest {

  @TempDir lateinit var tempDir: Path

  @Test fun `loads additional runtime dependency metadata`() {
    val coordinates = ModuleCoordinates("com.example:runtime", "1.0", GradleVariantIdentification.EMPTY)
    val dependency = ModuleDependency(coordinates, emptyMap())
    val dependenciesDir = dependenciesDir()
    dependenciesDir.asFile.resolve(coordinates.toFileName()).bufferWriteJson<Dependency>(dependency)

    val projectVariant = projectVariant()

    assertThat(projectVariant.dependencies(dependenciesDir, additionalClasspath = setOf(coordinates)))
      .containsExactly(dependency)
  }

  @Test fun `fails when additional runtime dependency metadata is missing`() {
    val coordinates = ModuleCoordinates("com.example:missing", "1.0", GradleVariantIdentification.EMPTY)
    val projectVariant = projectVariant()

    val ex = assertThrows(IllegalStateException::class.java) {
      projectVariant.dependencies(dependenciesDir(), additionalClasspath = setOf(coordinates))
    }

    assertThat(ex).hasMessageThat().contains("No file ${coordinates.toFileName()}")
  }

  private fun dependenciesDir() = ProjectBuilder.builder()
    .withProjectDir(tempDir.toFile())
    .build()
    .layout
    .projectDirectory
    .dir("dependencies")
    .also { it.asFile.mkdirs() }

  private fun projectVariant() = ProjectVariant(
    coordinates = ProjectCoordinates(":project", GradleVariantIdentification.EMPTY),
    buildType = null,
    flavor = null,
    sourceKind = JvmSourceKind.MAIN,
    sources = emptySet(),
    runtimeSources = emptySet(),
    classpath = emptySet(),
    annotationProcessors = emptySet(),
    testInstrumentationRunner = null,
    excludedIdentifiers = emptySet(),
  )
}
