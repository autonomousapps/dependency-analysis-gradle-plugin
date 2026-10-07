// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.internal.AggregateTypeUsageReport
import com.autonomousapps.model.internal.PublicTypes

/**
 * Answers one question: if project `A` stopped declaring project dependency `D`, would some other project stop
 * compiling?
 *
 * `D` is reachable from a consumer of `A` only because `A` exposes it. So when a consumer uses a type that `D`
 * publishes, "remove `D` from `A`" is a false positive.
 *
 * Build one with [of] and query it with [isTransitivelyExposed].
 */
internal class TransitiveExposureIndex(
  /** Project path -> the projects that consume it. */
  private val consumersOf: Map<String, Set<String>>,
  /** Project path -> every type that project uses, regardless of which dependency provides it. */
  private val typesUsedBy: Map<String, Set<String>>,
  /** Project path -> the types that project publishes. */
  private val typesPublishedBy: Map<String, Set<String>>,
  /** Project path -> the types that project leaks from its own dependencies, in its public ABI. */
  private val typesExposedBy: Map<String, Set<String>>,
  /** Project path -> the project dependencies it uses. */
  private val projectDepsOf: Map<String, Set<String>>,
  /** How many hops down the consumer graph to search. At least 1. */
  private val maxDepth: Int,
) {

  internal companion object {
    fun of(
      typeUsages: Collection<AggregateTypeUsageReport>,
      publicTypes: Collection<PublicTypes>,
      maxDepth: Int,
    ): TransitiveExposureIndex {
      val consumersOf = mutableMapOf<String, MutableSet<String>>()
      typeUsages.forEach { report ->
        report.projectDependencies.keys.forEach { producer ->
          consumersOf.getOrPut(producer) { mutableSetOf() }.add(report.projectPath)
        }
      }

      // A type may be attributed to a project dependency or to a library dependency depending on how the consumer
      // resolved it, so both are relevant.
      val typesUsedBy = typeUsages.associate { report ->
        report.projectPath to buildSet {
          report.projectDependencies.values.forEach { addAll(it) }
          report.libraryDependencies.values.forEach { addAll(it) }
        }
      }

      return TransitiveExposureIndex(
        consumersOf = consumersOf,
        typesUsedBy = typesUsedBy,
        typesPublishedBy = publicTypes.associate { it.projectPath to it.types },
        typesExposedBy = publicTypes.associate { it.projectPath to it.exposedTypes },
        projectDepsOf = typeUsages.associate { it.projectPath to it.projectDependencies.keys },
        maxDepth = maxDepth.coerceAtLeast(1),
      )
    }
  }

  /**
   * True when [declaringProject] reaches [dependencyProject]'s types through the public ABI of some *other*
   * dependency it uses.
   *
   * Where [isTransitivelyExposed] looks downstream at consumers, this looks upstream: if `P` uses `M`, and `M`'s ABI
   * leaks types that `D` publishes, then `P` compiles against `D`'s types via `M`. Dropping `D` from `P` is still
   * wrong, and no consumer of `P` need exist for that to be true.
   */
  fun isExposedThroughDependencyAbi(declaringProject: String, dependencyProject: String): Boolean {
    val publishedByDep = typesPublishedBy[dependencyProject].orEmpty()
    if (publishedByDep.isEmpty()) return false

    return projectDepsOf[declaringProject].orEmpty().any { usedDep ->
      if (usedDep == dependencyProject) return@any false
      typesExposedBy[usedDep].orEmpty().any { it in publishedByDep }
    }
  }

  /**
   * True when a consumer of [declaringProject], within [maxDepth] hops, uses a type published by [dependencyProject].
   * Breadth-first over the consumer graph.
   */
  fun isTransitivelyExposed(declaringProject: String, dependencyProject: String): Boolean {
    val exposedTypes = typesPublishedBy[dependencyProject].orEmpty()
    // Nothing is published, so nothing downstream can depend on it.
    if (exposedTypes.isEmpty()) return false

    val visited = mutableSetOf(declaringProject)
    val queue = ArrayDeque<Pair<String, Int>>()
    consumersOf[declaringProject]?.forEach { queue.add(it to 1) }

    while (queue.isNotEmpty()) {
      val (consumer, depth) = queue.removeFirst()
      if (!visited.add(consumer)) continue

      if (typesUsedBy[consumer].orEmpty().any { it in exposedTypes }) return true

      if (depth < maxDepth) {
        consumersOf[consumer]?.forEach { next ->
          if (next !in visited) queue.add(next to depth + 1)
        }
      }
    }

    return false
  }
}
