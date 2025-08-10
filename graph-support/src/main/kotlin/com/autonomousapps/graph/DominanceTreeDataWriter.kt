// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.graph

import com.autonomousapps.graph.DominanceTreeWriter.NodeWriter

public class DominanceTreeDataWriter<N : Any>(
  private val root: N,
  private val tree: DominanceTree<N>,
  private val nodeWriter: NodeWriter<N>,
) {

  public val sizeTree: DependencySizeTree<N> by lazy { dfs(root) }

  private fun dfs(node: N): DependencySizeTree<N> {
    return DependencySizeTree(
      self = node,
      size = nodeWriter.getSize(node),
      totalSize = nodeWriter.getTreeSize(node),
      dependencies = tree.dominanceGraph.successors(node)
        .run { nodeWriter.comparator()?.let { sortedWith(it) } ?: this }
        .map { dfs(it) }
    )
  }
}

public data class DependencySizeTree<N>(
  val self: N,
  val size: Long?,
  val totalSize: Long?,
  val dependencies: List<DependencySizeTree<N>>
) {
  public fun <M> map(f: (N) -> M): DependencySizeTree<M> = DependencySizeTree(
    self = f(self),
    size = size,
    totalSize = totalSize,
    dependencies = dependencies.map { it.map(f) }
  )
}

