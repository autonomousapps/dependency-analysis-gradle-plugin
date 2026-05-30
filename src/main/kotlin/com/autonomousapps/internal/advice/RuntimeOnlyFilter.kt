// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.graph.Graphs.root
import com.autonomousapps.internal.DependencyScope
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.mutPartitionOf
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.source.SourceKind

/**
 * Only permit add-runtimeOnly advice when there's also advice to remove a declaration that had been providing that
 * runtime usage transitively. For example:
 *
 * Before fixing dependencies:
 *   implementation(project(":unused")) (provides transitively: "com.group:runtime-capability:1.0")
 * After fixing dependencies:
 *   runtimeOnly("com.group:runtime-capability:1.0")
 *
 * ==OR==
 *
 * Before fixing dependencies:
 *   implementation(project(":used")) (provides transitively: "com.group:runtime-capability:1.0")
 * After fixing dependencies:
 *   implementation(project(":used")) (provides transitively: "com.group:runtime-capability:1.0")
 *
 * Which is to say (again): don't require users to add a bunch of `runtimeOnly(...)` declarations!
 */
internal class RuntimeOnlyFilter(
  private val dependencyGraph: Map<String, DependencyGraphView>,
  private val buildPath: String,
) {
  fun simplify(advice: Sequence<Advice>): Sequence<Advice> {
    val simplifiedAdvice = advice.toMutableList()

    val (removes, runtimeOnlys) = simplifiedAdvice.mutPartitionOf(
      { it.isAnyRemove() },
      { it.isAddToRuntimeOnly() },
    )
    val runtimeVisibleAdvice = simplifiedAdvice.filter { it.isRuntimeVisible() }

    // If there is no advice to remove anything, then as an optimization we can drop all the add-to-runtimeOnly advice.
    if (removes.isEmpty()) {
      simplifiedAdvice.removeAll(runtimeOnlys)
    } else {
      runtimeOnlys
        .forEach { runtimeOnly ->
          // Get the source set associated with this add-advice.
          val sourceSetName = DependencyScope.sourceSetName(runtimeOnly.toConfiguration!!)
            ?: error("Couldn't determine source set name for '${runtimeOnly.toConfiguration}'")

          // Now find the graph associated with that source set
          // A null graph is invalid because we will only advise adding a runtimeOnly dependency if it's already present
          // on a runtime graph.
          val graph = dependencyGraph.values.firstOrNull { graph ->
            graph.sourceKind.sourceSetMatches(sourceSetName) && graph.isRuntime()
          } ?: error("Couldn't find a runtime graph associated with:\n  $runtimeOnly")

          // First, find the direct nodes that are bringing this in
          val root = graph.graph.root()
          val directs = graph.graph.children(root)
            // Sometimes the graph has a node for the root twice: once as a ProjectCoordinates, and once as an
            // IncludedBuildCoordinates. This can lead to incorrect results.
            .filterNot { node -> node.normalizedIdentifier(buildPath) == root.normalizedIdentifier(buildPath) }
            .filterToSet { direct -> graph.graph.reachableNodes(direct).anyMatch(runtimeOnly) }

          // Now, see if any of those direct nodes are being removed
          val matches = removes.filterToSet { remove -> directs.anyMatch(remove) }

          // The removed direct node might also be replaced by advice that still brings this runtime dependency
          // transitively, such as a plugin marker replacing an intermediate project dependency.
          val hasReplacement = runtimeVisibleAdvice.any { replacement ->
            if (!replacement.isRuntimeVisibleTo(graph.sourceKind)) return@any false

            graph.graph.nodes().matching(replacement)?.let { node ->
              graph.graph.reachableNodes(node).anyMatch(runtimeOnly)
            } == true
          }

          // TODO(tsr): androidTest... doesn't extend from test source. Double-check various assumptions about this.
          // If ALL the direct nodes are being removed, then we can keep the runtimeOnly advice. Otherwise, remove it.
          val bothEmpty = matches.isEmpty() && directs.isEmpty()
          if (hasReplacement || bothEmpty || matches.size != directs.size) {
            simplifiedAdvice.remove(runtimeOnly)
          }
        }
    }

    return simplifiedAdvice.asSequence()
  }

  // We can't match directly on Coordinates, since Advice.coordinates is not GradleVariantIdentification-aware.
  private fun Set<Coordinates>.anyMatch(other: Advice): Boolean {
    return any { it.normalizedIdentifier(buildPath) == other.coordinates.normalizedIdentifier(buildPath) }
  }

  private fun Set<Coordinates>.matching(other: Advice): Coordinates? {
    return firstOrNull { it.normalizedIdentifier(buildPath) == other.coordinates.normalizedIdentifier(buildPath) }
  }

  private fun Advice.isRuntimeVisible(): Boolean {
    if (isAddToRuntimeOnly()) return false

    if (!isAnyAdd() && !isAnyChange()) return false

    return toConfiguration?.let {
      it.endsWith("api", ignoreCase = true)
        || it.endsWith("implementation", ignoreCase = true)
        || it.endsWith("runtimeOnly", ignoreCase = true)
    } == true
  }

  private fun Advice.isRuntimeVisibleTo(sourceKind: SourceKind): Boolean {
    if (!isRuntimeVisible()) return false

    val sourceSetName = DependencyScope.sourceSetName(toConfiguration!!) ?: return false
    if (sourceKind.sourceSetMatches(sourceSetName)) return true

    return sourceSetName == SourceKind.MAIN_NAME && sourceKind.kind in MAIN_VISIBLE_TO_RUNTIME_SOURCE_KINDS
  }

  private companion object {
    private val MAIN_VISIBLE_TO_RUNTIME_SOURCE_KINDS = setOf(
      SourceKind.TEST_KIND,
      SourceKind.ANDROID_TEST_FIXTURES_KIND,
      SourceKind.ANDROID_TEST_KIND,
    )
  }
}
