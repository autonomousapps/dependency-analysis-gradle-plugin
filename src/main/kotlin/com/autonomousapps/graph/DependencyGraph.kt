package com.autonomousapps.graph

internal class DependencyGraph {

  private var edgeCount = 0
  private val adj = LinkedHashMap<String, MutableSet<Edge>>()
  private val nodes = LinkedHashMap<String, Node>()
  private val inDegree = LinkedHashMap<String, Int>()

  companion object {
    fun newGraph(edges: Iterable<Edge>): DependencyGraph {
      val graph = DependencyGraph()
      edges.forEach {
        graph.addEdge(it)
      }
      return graph
    }
  }

  fun addEdge(edge: Edge) {
    var added = true

    // Update adjacency lists with new edge
    adj.merge(edge.from.identifier, mutableSetOf(edge)) { set, increment ->
      set.apply { added = addAll(increment) }
    }
    // Ensure every node, including leaves, is in the adjacency map
    adj.computeIfAbsent(edge.to.identifier) { mutableSetOf() }

    // Updated mapping of node identifier -> node
    nodes.computeIfAbsent(edge.from.identifier) { edge.from }
    nodes.computeIfAbsent(edge.to.identifier) { edge.to }

    // Only increment these things if we actually added a new edge. This graph does not support
    // parallel edges
    if (added) {
      inDegree.merge(edge.to.identifier, 1) { old, new -> old + new }
      edgeCount++
    }

    // ensure every node has some in-degree value set
    inDegree.computeIfAbsent(edge.from.identifier) { 0 }
    inDegree.computeIfAbsent(edge.to.identifier) { 0 }
  }

  /**
   * The root node. That is, the node with an in-degree of 0. (There should be only one!)
   */
  val rootNode: Node by lazy {
    val root = inDegree.entries.find { (_, inDegree) ->
      inDegree == 0
    }?.key ?: error("could not find root node")

    nodes[root] ?: missingNode(root)
  }

  fun nodeCount(): Int = adj.size
  fun edgeCount(): Int = edgeCount

  /**
   * Returns the edges adjacent from node `from` in this digraph.
   *
   * @param  from the node
   * @return the edges adjacent from node `from` in this digraph, as an iterable
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun adj(from: Node): Iterable<Edge> = adj(from.identifier)

  /**
   * Returns the edges adjacent from node `from` in this digraph.
   *
   * @param  from the node
   * @return the edges adjacent from node `from` in this digraph, as an iterable
   * @throws IllegalArgumentException unless `from` is in the graph.
   */
  fun adj(from: String): Iterable<Edge> = adj[from] ?: missingNode(from)

  fun edges(): List<Edge> = nodes.flatMap { (_, node) ->
    adj(node)
  }

  fun nodes(): Iterable<Node> = nodes.map { (_, node) -> node }

  fun hasNode(node: Node): Boolean = hasNode(node.identifier)
  fun hasNode(node: String): Boolean = nodes[node] != null
}

internal fun missingNode(node: Node): Nothing = missingNode(node.identifier)
internal fun missingNode(node: String): Nothing =
  throw IllegalArgumentException("Node $node is not in the graph")
