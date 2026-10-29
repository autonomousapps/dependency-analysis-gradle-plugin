// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.graph.Graphs.reversed
import com.autonomousapps.graph.Graphs.topological
import com.autonomousapps.internal.utils.mapNotNullToSet
import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.source.KmpSourceKind
import com.autonomousapps.model.source.SourceKind
import com.google.common.graph.Graph

internal class KmpSourceSetNameMatcher(
  private val kmpSourceGraph: Graph<String>?,
  val primarySourceKind: SourceKind,
) {

  /**
   * Finds the lowest common ancestor of all the source sets represented in [classReferences], and returns it.
   *
   * @see <a href="https://kotlinlang.org/docs/multiplatform/multiplatform-hierarchy.html#modifying-source-sets">Modifying source sets</a>
   */
  fun getLowestCommonAncestor(classReferences: Set<ClassReference>): SourceKind {
    // If not a KMP project, use the fallback.
    if (primarySourceKind !is KmpSourceKind) return primarySourceKind

    // If there are no class references, we can just use the fallback.
    if (classReferences.isEmpty()) return primarySourceKind

    // Optimization. If there's only a single class reference, it is already the LCA.
    if (classReferences.size == 1) {
      val lca = classReferences.first().kmpSourceSetName
      // This probably can't be null, but whatever.
      if (lca != null) {
        return if (lca == primarySourceKind.name) {
          primarySourceKind
        } else {
          primarySourceKind.copy(name = lca)
        }
      }
    }

    /*
     * Hard work below.
     */

    // We allow a null `kmpSourceGraph` to be passed in so callers have an easier time. It must't be null at this point.
    val sourceGraph = requireNotNull(kmpSourceGraph) { "KMP SourceGraph was null" }
    val kmpSourceSets = classReferences.mapNotNullToSet { it.kmpSourceSetName }

    // TODO(tsr): this LCA code could move to graph-support

    // Find lowest common ancestor
    val ancestors = kmpSourceSets
      .map { sourceSet -> sourceGraph.reachableNodes(sourceSet, false) }
      .reduce { acc, nodes -> acc.intersect(nodes) }

    // If there's only one ancestor, it's the LCA. Otherwise, we do a topological sort, intersect with the ancestors,
    // and choose the node furthest from the root. Note we have to reverse the graph first because the graph starts out
    // reversed.
    val lca = if (ancestors.size == 1) {
      ancestors.first()
    } else {
      sourceGraph.reversed().topological("commonMain").intersect(ancestors).last()
    }

    // If the fallback and the LCA are the same, just return the fallback as-is. Otherwise, replace it.
    return if (lca == primarySourceKind.name) {
      primarySourceKind
    } else {
      primarySourceKind.copy(name = lca)
    }
  }
}
