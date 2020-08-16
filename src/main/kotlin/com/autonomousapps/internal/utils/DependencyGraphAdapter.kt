package com.autonomousapps.internal.utils

import com.autonomousapps.graph.*
import com.squareup.moshi.*

internal class DependencyGraphAdapter {

  @ToJson fun fromGraph(graph: DependencyGraph): List<Edge> {
    return graph.edges()
  }

  @FromJson fun toGraph(edges: List<Edge>): DependencyGraph {
    return DependencyGraph.newGraph(edges)
  }

  @ToJson fun fromNode(
    writer: JsonWriter,
    node: Node,
    consumerDelegate: JsonAdapter<ConsumerNode>, producerDelegate: JsonAdapter<ProducerNode>
  ): Unit = when (node) {
    is ConsumerNode -> consumerDelegate.toJson(writer, node)
    is ProducerNode -> producerDelegate.toJson(writer, node)
  }

  @FromJson fun toNode(
    reader: JsonReader,
    consumerDelegate: JsonAdapter<ConsumerNode>, producerDelegate: JsonAdapter<ProducerNode>
  ): Node? = try {
    // Most nodes will be ProducerNodes
    producerDelegate.fromJson(reader)
  } catch (_: Exception) {
    consumerDelegate.fromJson(reader)
  }
}

internal data class GraphJson(
  /**
   * A mapping of node-identifier to set-of-node-identifiers.
   */
  val map: Map<String, Set<String>>,
  /**
   * A mapping of node-identifier to node.
   */
  val nodes: Map<String, Node>
)

// I have a hard time letting go of this time-consuming-to-write code...
//  @ToJson fun fromGraph(graph: DependencyGraph): GraphJson {
//    val map = graph.map()
//    val stringMap = mutableMapOf<String, Set<String>>()
//    val nodes = mutableMapOf<String, Node>()
//    map.forEach { (from, tos) ->
//      stringMap[from.identifier] = tos.mapToSet { to -> to.identifier }
//      nodes.putIfAbsent(from.identifier, from)
//      tos.forEach { to ->
//        nodes.putIfAbsent(to.identifier, to)
//      }
//    }
//    return GraphJson(stringMap, nodes)
//  }
//
//  @FromJson fun toGraph(json: GraphJson): DependencyGraph {
//    val map = mutableMapOf<Node, Set<Node>>()
//    json.map.forEach { (from, tos) ->
//      val fromNode = json.nodes[from] ?: error("No node found for $from")
//      val toNodes = tos.mapToSet { to -> json.nodes[to] ?: error("No node found for $to") }
//      map[fromNode] = toNodes
//    }
//    return DependencyGraph.newGraph(map)
//  }