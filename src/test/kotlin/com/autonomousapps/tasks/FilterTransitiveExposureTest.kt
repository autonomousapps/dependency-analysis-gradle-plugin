// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.PublicTypes
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class FilterTransitiveExposureTest {

  private val emptyGvi = GradleVariantIdentification.EMPTY

  @Test fun `dep used by downstream consumer is suppressed`() {
    // Project :A declares dep :B as implementation (unused in A's source).
    // Project :C depends on :A and uses classes from :B.
    // → Removing :B from :A would break :C.

    val publicTypesB = PublicTypes(
      projectPath = ":B",
      types = setOf("com.example.b.FooService", "com.example.b.BarService")
    )

    // :C uses FooService from :B (accessed transitively through :A)
    val typeUsageC = AggregateTypeUsageReport(
      projectPath = ":C",
      internal = emptySet(),
      projectDependencies = mapOf(
        ":A" to setOf("com.example.a.SomeClass"),
        ":B" to setOf("com.example.b.FooService")  // C uses B's class
      ),
      libraryDependencies = emptyMap()
    )

    // :A's advice says to remove :B
    val adviceRemoveB = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )

    val projectAdviceA = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(adviceRemoveB)
    )

    // Run the detection algorithm
    val result = filterTransitiveExposures(
      allAdvice = listOf(projectAdviceA),
      typeUsageByProject = mapOf(":C" to typeUsageC),
      publicClassesByProject = mapOf(":B" to publicTypesB)
    )

    // The "remove :B" advice should be suppressed
    assertThat(result).hasSize(1)
    assertThat(result[0].dependencyAdvice).isEmpty()
  }

  @Test fun `dep NOT used by any consumer is kept`() {
    // Project :A declares dep :B as implementation (unused).
    // No one depends on :A, or consumers don't use :B's classes.
    // → Safe to remove :B.

    val publicTypesB = PublicTypes(
      projectPath = ":B",
      types = setOf("com.example.b.FooService")
    )

    val typeUsageC = AggregateTypeUsageReport(
      projectPath = ":C",
      internal = emptySet(),
      projectDependencies = mapOf(
        ":A" to setOf("com.example.a.SomeClass")
        // :C does NOT use any class from :B
      ),
      libraryDependencies = emptyMap()
    )

    val adviceRemoveB = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )

    val projectAdviceA = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(adviceRemoveB)
    )

    val result = filterTransitiveExposures(
      allAdvice = listOf(projectAdviceA),
      typeUsageByProject = mapOf(":C" to typeUsageC),
      publicClassesByProject = mapOf(":B" to publicTypesB)
    )

    // The "remove :B" advice should be kept (safe to remove)
    assertThat(result).hasSize(1)
    assertThat(result[0].dependencyAdvice).containsExactly(adviceRemoveB)
  }

  @Test fun `non-removal advice is never filtered`() {
    // Change advice (e.g., impl → api) should never be suppressed

    val adviceChange = Advice.ofChange(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation",
      toConfiguration = "api"
    )

    val projectAdviceA = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(adviceChange)
    )

    val result = filterTransitiveExposures(
      allAdvice = listOf(projectAdviceA),
      typeUsageByProject = emptyMap(),
      publicClassesByProject = emptyMap()
    )

    assertThat(result[0].dependencyAdvice).containsExactly(adviceChange)
  }

  @Test fun `project with no consumers keeps all removal advice`() {
    val publicTypesB = PublicTypes(
      projectPath = ":B",
      types = setOf("com.example.b.FooService")
    )

    val adviceRemoveB = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )

    val projectAdviceA = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(adviceRemoveB)
    )

    // No type usage data for any consumer of :A
    val result = filterTransitiveExposures(
      allAdvice = listOf(projectAdviceA),
      typeUsageByProject = emptyMap(),
      publicClassesByProject = mapOf(":B" to publicTypesB)
    )

    // No consumers → no transitive exposure → keep advice
    assertThat(result[0].dependencyAdvice).containsExactly(adviceRemoveB)
  }

  /**
   * Extracted algorithm matching FilterTransitiveExposureTask.Action logic.
   * Used for unit testing without Gradle task infrastructure.
   */
  private fun filterTransitiveExposures(
    allAdvice: List<ProjectAdvice>,
    typeUsageByProject: Map<String, AggregateTypeUsageReport>,
    publicClassesByProject: Map<String, PublicTypes>,
  ): List<ProjectAdvice> {
    // Build reverse dep graph from type usage data
    val dependedBy = mutableMapOf<String, MutableSet<String>>()
    typeUsageByProject.forEach { (accessingProject, report) ->
      report.projectDependencies.keys.forEach { depProject ->
        dependedBy.getOrPut(depProject) { mutableSetOf() }.add(accessingProject)
      }
    }

    return allAdvice.map { projectAdvice ->
      val projectPath = projectAdvice.projectPath
      val consumers = dependedBy[projectPath].orEmpty()

      if (consumers.isEmpty()) return@map projectAdvice

      val filteredAdvice = projectAdvice.dependencyAdvice.filter { advice ->
        if (!advice.isAnyRemove()) return@filter true

        val depCoords = advice.coordinates
        if (depCoords !is ProjectCoordinates) return@filter true

        val depProjectPath = depCoords.identifier
        val depPublicClasses = publicClassesByProject[depProjectPath]?.types.orEmpty()
        if (depPublicClasses.isEmpty()) return@filter true

        val isTransitivelyExposed = consumers.any { consumerPath ->
          val consumerUsage = typeUsageByProject[consumerPath] ?: return@any false
          val allClassesConsumerUses = consumerUsage.projectDependencies.values
            .flatMapTo(mutableSetOf()) { it }
          depPublicClasses.any { it in allClassesConsumerUses }
        }

        !isTransitivelyExposed
      }.toSet()

      projectAdvice.copy(dependencyAdvice = filteredAdvice)
    }
  }
}
