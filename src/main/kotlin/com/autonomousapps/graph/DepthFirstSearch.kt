package com.autonomousapps.graph

/**
 * This class is used to generate subgraphs from a larger graph, [graph], which are reachable from
 * the given source node.
 */
internal class DepthFirstSearch(
  private val graph: DependencyGraph,
  source: Node
) {

  private val marked = linkedMapOf<String, Boolean>()

  init {
    dfs(source)
  }

  private fun dfs(source: Node) {
    marked[source.identifier] = true
    graph.adj(source).forEach { (_, to) ->
      if (!isMarked(to)) {
        dfs(to)
      }
    }
  }

  val subgraph: DependencyGraph by lazy {
    val sub = DependencyGraph()
    graph.edges().forEach {
      // if both nodes on the edge are marked, add this edge to the subgraph
      if (isMarked(it.from) && isMarked(it.to)) {
        sub.addEdge(it)
      }
    }
    sub
  }

  private fun isMarked(node: Node): Boolean = isMarked(node.identifier)
  private fun isMarked(identifier: String): Boolean = marked.getOrDefault(identifier, false)
}