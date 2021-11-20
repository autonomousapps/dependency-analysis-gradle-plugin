package com.autonomousapps.internal.utils

import com.autonomousapps.graph.*
import com.squareup.moshi.*

@Suppress("unused")
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
    bareDelegate: JsonAdapter<BareNode>,
    consumerDelegate: JsonAdapter<ConsumerNode>,
    producerDelegate: JsonAdapter<ProducerNode>
  ): Unit = when (node) {
    is BareNode -> bareDelegate.toJson(writer, node)
    is ConsumerNode -> consumerDelegate.toJson(writer, node)
    is ProducerNode -> producerDelegate.toJson(writer, node)
  }

  @FromJson fun toNode(
    reader: JsonReader,
    bareDelegate: JsonAdapter<BareNode>,
    consumerDelegate: JsonAdapter<ConsumerNode>,
    producerDelegate: JsonAdapter<ProducerNode>
  ): Node? = try {
    // Most nodes will be ProducerNodes
    producerDelegate.fromJson(reader)
  } catch (_: Exception) {
    // TODO fix this atrocity
    try {
      consumerDelegate.fromJson(reader)
    } catch (_: Exception) {
      bareDelegate.fromJson(reader)
    }
  }
}
