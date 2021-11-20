package com.autonomousapps.graph

/**
 * Finds the topological order of DAG [graph]. We know it has one, because it is a DAG.
 *
 * @param graph the DAG.
 */
internal class Topological(
  private val graph: DependencyGraph
) {

  private val order: Iterable<String>

  init {
    val dfs = DepthFirstOrder(graph)
    order = dfs.reversePost()
  }

  /**
   * Returns a topological order for the digraph.
   *
   * @return a topological order of the nodes (as an iterable).
   */
  fun order(): Iterable<String> = order
}
