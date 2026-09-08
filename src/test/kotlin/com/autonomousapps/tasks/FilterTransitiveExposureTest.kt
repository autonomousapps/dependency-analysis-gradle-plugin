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

  @Test fun `multi-hop consumer is only caught when depth allows it`() {
    // Chain: :A declares :B (unused in A). :C depends on :A (but does NOT use :B).
    // :D depends on :C and DOES use :B's class. So :B is exposed 2 hops from :A.
    val publicTypesB = PublicTypes(
      projectPath = ":B",
      types = setOf("com.example.b.FooService")
    )
    // :C consumes :A only
    val typeUsageC = AggregateTypeUsageReport(
      projectPath = ":C", internal = emptySet(),
      projectDependencies = mapOf(":A" to setOf("com.example.a.SomeClass")),
      libraryDependencies = emptyMap()
    )
    // :D consumes :C and uses :B's FooService
    val typeUsageD = AggregateTypeUsageReport(
      projectPath = ":D", internal = emptySet(),
      projectDependencies = mapOf(":C" to setOf("com.example.b.FooService")),
      libraryDependencies = emptyMap()
    )
    val adviceRemoveB = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )
    val projectAdviceA = ProjectAdvice(projectPath = ":A", dependencyAdvice = setOf(adviceRemoveB))
    val usage = mapOf(":C" to typeUsageC, ":D" to typeUsageD)
    val pub = mapOf(":B" to publicTypesB)

    // Depth 1: only direct consumer :C is checked; :C doesn't use :B → NOT exposed → advice kept.
    val depth1 = filterTransitiveExposures(listOf(projectAdviceA), usage, pub, maxDepth = 1)
    assertThat(depth1[0].dependencyAdvice).containsExactly(adviceRemoveB)

    // Depth 2: :D (2 hops) is checked; :D uses :B → exposed → advice suppressed.
    val depth2 = filterTransitiveExposures(listOf(projectAdviceA), usage, pub, maxDepth = 2)
    assertThat(depth2[0].dependencyAdvice).isEmpty()
  }

  @Test fun `coordinate include filter keeps only matching advice`() {
    val removeProject = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )
    val removeExternal = Advice.ofRemove(
      coordinates = com.autonomousapps.model.ModuleCoordinates("org.springframework:spring-core", "6.0.0", emptyGvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(removeProject, removeExternal)
    )

    // Include only internal project deps.
    val included = applyCoordinateFilter(
      listOf(projectAdvice),
      include = listOf("^:.*"),
      exclude = emptyList()
    )
    assertThat(included[0].dependencyAdvice).containsExactly(removeProject)
  }

  @Test fun `coordinate exclude filter drops matching advice`() {
    val removeProject = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )
    val removeExternal = Advice.ofRemove(
      coordinates = com.autonomousapps.model.ModuleCoordinates("org.springframework:spring-core", "6.0.0", emptyGvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(
      projectPath = ":A",
      dependencyAdvice = setOf(removeProject, removeExternal)
    )

    val filtered = applyCoordinateFilter(
      listOf(projectAdvice),
      include = emptyList(),
      exclude = listOf("^org\\.springframework.*")
    )
    assertThat(filtered[0].dependencyAdvice).containsExactly(removeProject)
  }

  @Test fun `empty coordinate filter is a no-op`() {
    val removeProject = Advice.ofRemove(
      coordinates = ProjectCoordinates(":B", emptyGvi),
      fromConfiguration = "implementation"
    )
    val projectAdvice = ProjectAdvice(projectPath = ":A", dependencyAdvice = setOf(removeProject))
    val out = applyCoordinateFilter(listOf(projectAdvice), include = emptyList(), exclude = emptyList())
    assertThat(out[0].dependencyAdvice).containsExactly(removeProject)
  }

  /** Mirrors the coordinate include/exclude logic in FilterTransitiveExposureTask.Action. */
  private fun applyCoordinateFilter(
    allAdvice: List<ProjectAdvice>,
    include: List<String>,
    exclude: List<String>,
  ): List<ProjectAdvice> {
    val includeRegexes = include.map { it.toRegex() }
    val excludeRegexes = exclude.map { it.toRegex() }
    fun included(identifier: String): Boolean {
      val inc = includeRegexes.isEmpty() || includeRegexes.any { it.containsMatchIn(identifier) }
      if (!inc) return false
      return excludeRegexes.none { it.containsMatchIn(identifier) }
    }
    if (includeRegexes.isEmpty() && excludeRegexes.isEmpty()) return allAdvice
    return allAdvice.map { pa ->
      pa.copy(dependencyAdvice = pa.dependencyAdvice.filterTo(mutableSetOf()) { included(it.coordinates.identifier) })
    }
  }

  /**
   * Extracted algorithm matching FilterTransitiveExposureTask.Action logic.
   * Used for unit testing without Gradle task infrastructure.
   */
  private fun filterTransitiveExposures(
    allAdvice: List<ProjectAdvice>,
    typeUsageByProject: Map<String, AggregateTypeUsageReport>,
    publicClassesByProject: Map<String, PublicTypes>,
    maxDepth: Int = 1,
  ): List<ProjectAdvice> {
    // Build reverse dep graph from type usage data
    val dependedBy = mutableMapOf<String, MutableSet<String>>()
    typeUsageByProject.forEach { (accessingProject, report) ->
      report.projectDependencies.keys.forEach { depProject ->
        dependedBy.getOrPut(depProject) { mutableSetOf() }.add(accessingProject)
      }
    }

    fun isUsedByConsumers(projectPath: String, depPublicClasses: Set<String>): Boolean {
      val visited = mutableSetOf<String>()
      val queue = ArrayDeque<Pair<String, Int>>()
      dependedBy[projectPath]?.forEach { queue.add(it to 1) }
      while (queue.isNotEmpty()) {
        val (current, depth) = queue.removeFirst()
        if (!visited.add(current)) continue
        val usage = typeUsageByProject[current]
        if (usage != null) {
          val classes = usage.projectDependencies.values.flatMapTo(mutableSetOf()) { it }
          if (depPublicClasses.any { it in classes }) return true
        }
        if (depth < maxDepth) {
          dependedBy[current]?.forEach { next -> if (next !in visited) queue.add(next to depth + 1) }
        }
      }
      return false
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

        !isUsedByConsumers(projectPath, depPublicClasses)
      }.toSet()

      projectAdvice.copy(dependencyAdvice = filteredAdvice)
    }
  }
}
