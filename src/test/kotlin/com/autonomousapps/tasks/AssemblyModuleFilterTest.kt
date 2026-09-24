// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.internal.utils.fromJson
import com.autonomousapps.internal.utils.toJson
import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.ProjectMetadata
import com.autonomousapps.model.internal.ProjectType
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class AssemblyModuleFilterTest {

  private val emptyGvi = GradleVariantIdentification.EMPTY

  @Test fun `assembly module has all removal advice suppressed`() {
    val assemblyProjects = setOf(":app-shadow")

    val adviceRemoveA = Advice.ofRemove(
      coordinates = ProjectCoordinates(":lib-a", emptyGvi),
      fromConfiguration = "implementation"
    )
    val adviceRemoveB = Advice.ofRemove(
      coordinates = ProjectCoordinates(":lib-b", emptyGvi),
      fromConfiguration = "implementation"
    )
    val adviceChange = Advice.ofChange(
      coordinates = ProjectCoordinates(":lib-c", emptyGvi),
      fromConfiguration = "implementation",
      toConfiguration = "api"
    )

    val projectAdvice = ProjectAdvice(
      projectPath = ":app-shadow",
      dependencyAdvice = setOf(adviceRemoveA, adviceRemoveB, adviceChange)
    )

    val result = filterAssemblyModuleAdvice(
      projectAdvice = projectAdvice,
      assemblyProjects = assemblyProjects
    )

    // Removal advice suppressed, change advice kept
    assertThat(result.dependencyAdvice).containsExactly(adviceChange)
  }

  @Test fun `non-assembly module keeps all advice`() {
    val assemblyProjects = setOf(":other-shadow-module")

    val adviceRemoveA = Advice.ofRemove(
      coordinates = ProjectCoordinates(":lib-a", emptyGvi),
      fromConfiguration = "implementation"
    )
    val adviceChange = Advice.ofChange(
      coordinates = ProjectCoordinates(":lib-c", emptyGvi),
      fromConfiguration = "implementation",
      toConfiguration = "api"
    )

    val projectAdvice = ProjectAdvice(
      projectPath = ":regular-module",
      dependencyAdvice = setOf(adviceRemoveA, adviceChange)
    )

    val result = filterAssemblyModuleAdvice(
      projectAdvice = projectAdvice,
      assemblyProjects = assemblyProjects
    )

    assertThat(result.dependencyAdvice).containsExactly(adviceRemoveA, adviceChange)
  }

  @Test fun `assembly module with no removal advice is unchanged`() {
    val assemblyProjects = setOf(":app-shadow")

    val adviceChange = Advice.ofChange(
      coordinates = ProjectCoordinates(":lib-c", emptyGvi),
      fromConfiguration = "implementation",
      toConfiguration = "api"
    )
    val adviceAdd = Advice.ofAdd(
      coordinates = ProjectCoordinates(":lib-d", emptyGvi),
      toConfiguration = "implementation"
    )

    val projectAdvice = ProjectAdvice(
      projectPath = ":app-shadow",
      dependencyAdvice = setOf(adviceChange, adviceAdd)
    )

    val result = filterAssemblyModuleAdvice(
      projectAdvice = projectAdvice,
      assemblyProjects = assemblyProjects
    )

    assertThat(result.dependencyAdvice).containsExactly(adviceChange, adviceAdd)
  }

  @Test fun `ProjectMetadata with isAssembly=true round-trips through JSON`(@TempDir tempDir: File) {
    val metadata = ProjectMetadata(
      projectPath = ":serverless-sail-evaluator",
      projectType = ProjectType.JVM,
      isAssembly = true,
    )

    val json = metadata.toJson()
    val file = tempDir.resolve("metadata.json")
    file.writeText(json)
    val deserialized = file.fromJson<ProjectMetadata>()

    assertThat(deserialized.projectPath).isEqualTo(":serverless-sail-evaluator")
    assertThat(deserialized.projectType).isEqualTo(ProjectType.JVM)
    assertThat(deserialized.isAssembly).isTrue()
  }

  @Test fun `ProjectMetadata with isAssembly=false round-trips through JSON`(@TempDir tempDir: File) {
    val metadata = ProjectMetadata(
      projectPath = ":regular-lib",
      projectType = ProjectType.JVM,
      isAssembly = false,
    )

    val json = metadata.toJson()
    val file = tempDir.resolve("metadata.json")
    file.writeText(json)
    val deserialized = file.fromJson<ProjectMetadata>()

    assertThat(deserialized.projectPath).isEqualTo(":regular-lib")
    assertThat(deserialized.projectType).isEqualTo(ProjectType.JVM)
    assertThat(deserialized.isAssembly).isFalse()
  }

  @Test fun `ProjectMetadata without isAssembly field in JSON defaults to false`(@TempDir tempDir: File) {
    // Simulate reading a JSON file from before isAssembly was added (backward compat)
    val json = """{"projectPath":":old-module","projectType":"JVM"}"""
    val file = tempDir.resolve("metadata.json")
    file.writeText(json)
    val deserialized = file.fromJson<ProjectMetadata>()

    assertThat(deserialized.projectPath).isEqualTo(":old-module")
    assertThat(deserialized.projectType).isEqualTo(ProjectType.JVM)
    assertThat(deserialized.isAssembly).isFalse()
  }

  /**
   * Extracted pure function that mirrors the assembly-module filtering logic
   * in FilterTransitiveExposureTask.Action.execute().
   * Suppresses all removal advice for projects identified as assembly modules.
   */
  companion object {
    @JvmStatic
    fun filterAssemblyModuleAdvice(
      projectAdvice: ProjectAdvice,
      assemblyProjects: Set<String>,
    ): ProjectAdvice {
      val projectPath = projectAdvice.projectPath
      if (projectPath !in assemblyProjects) return projectAdvice

      val filtered = projectAdvice.dependencyAdvice.filterTo(mutableSetOf()) { advice ->
        !advice.isAnyRemove()
      }
      return projectAdvice.copy(dependencyAdvice = filtered)
    }
  }
}
