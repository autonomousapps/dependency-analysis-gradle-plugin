// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.graph.utils

import com.google.common.graph.ElementOrder
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

val MOSHI: Moshi by lazy {
  Moshi.Builder()
    .add(GraphAdapter())
    .addLast(KotlinJsonAdapterFactory())
    .build()
}

inline fun <reified T> JsonAdapter<T>.withNulls(withNulls: Boolean): JsonAdapter<T> {
  return if (withNulls) {
    serializeNulls()
  } else {
    this
  }
}

inline fun <reified T> getJsonAdapter(withNulls: Boolean = false): JsonAdapter<T> {
  return MOSHI.adapter(T::class.java).withNulls(withNulls)
}

inline fun <reified T> String.fromJson(): T {
  return getJsonAdapter<T>().fromJson(this)!!
}

inline fun <reified T> T.toJson(withNulls: Boolean = false): String {
  return getJsonAdapter<T>(withNulls).toJson(this)
}

@Suppress("unused")
internal class GraphAdapter {

  @ToJson fun stringGraphContainerToJson(graph: StringGraphContainer): StringGraphJson {
    return StringGraphJson(
      nodes = graph.graph.nodes().toSortedSet(),
      edges = graph.graph.edges().asSequence().map { pair ->
        pair.nodeU() to pair.nodeV()
      }.toSortedSet()
    )
  }

  @FromJson fun jsonToStringGraphContainer(json: StringGraphJson): StringGraphContainer {
    val graphBuilder = newGraphBuilder<String>()
    json.nodes.forEach { graphBuilder.addNode(it) }
    json.edges.forEach { (source, target) -> graphBuilder.putEdge(source, target) }
    val graph = graphBuilder.build()

    return StringGraphContainer(graph)
  }

  private infix fun String.to(target: String) = StringEdgeJson(this, target)

  @JsonClass(generateAdapter = false)
  internal data class StringGraphContainer(val graph: Graph<String>)

  @JsonClass(generateAdapter = false)
  internal data class StringGraphJson(
    val nodes: Set<String>,
    val edges: Set<StringEdgeJson>,
  )

  @JsonClass(generateAdapter = false)
  internal data class StringEdgeJson(val source: String, val target: String) : Comparable<StringEdgeJson> {
    override fun compareTo(other: StringEdgeJson): Int {
      return compareBy(StringEdgeJson::source)
        .thenComparing(StringEdgeJson::target)
        .compare(this, other)
    }
  }

  private fun <T> newGraphBuilder(): ImmutableGraph.Builder<T> {
    return GraphBuilder.directed()
      .allowsSelfLoops(false)
      .incidentEdgeOrder(ElementOrder.stable<T>())
      .immutable()
  }
}
