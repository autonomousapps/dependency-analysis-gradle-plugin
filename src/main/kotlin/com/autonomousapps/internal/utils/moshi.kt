// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:JvmName("MoshiUtils")
@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.source.SourceKind
import com.google.common.graph.Graph
import com.squareup.moshi.*
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshix.sealed.reflect.MoshiSealedJsonAdapterFactory
import okio.*
import org.gradle.api.file.RegularFileProperty
import java.io.File
import kotlin.io.use

const val noJsonIndent = ""

val MOSHI: Moshi by lazy {
  Moshi.Builder()
    .add(GraphAdapter())
    .add(MoshiSealedJsonAdapterFactory())
    .add(TypeAdapters())
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

inline fun <reified T> getJsonListAdapter(withNulls: Boolean = false): JsonAdapter<List<T>> {
  val type = newParameterizedType(List::class.java, T::class.java)
  return MOSHI.adapter<List<T>>(type).withNulls(withNulls)
}

inline fun <reified T> getJsonSetAdapter(withNulls: Boolean = false): JsonAdapter<Set<T>> {
  val type = newParameterizedType(Set::class.java, T::class.java)
  return MOSHI.adapter<Set<T>>(type).withNulls(withNulls)
}

inline fun <reified K, reified V> getJsonMapAdapter(withNulls: Boolean = false): JsonAdapter<Map<K, V>> {
  val type = newParameterizedType(Map::class.java, K::class.java, V::class.java)
  return MOSHI.adapter<Map<K, V>>(type).withNulls(withNulls)
}

inline fun <reified K, reified V> getJsonMapSetAdapter(withNulls: Boolean = false): JsonAdapter<Map<K, Set<V>>> {
  val setType = newParameterizedType(Set::class.java, V::class.java)
  val mapType = newParameterizedType(Map::class.java, K::class.java, setType)
  return MOSHI.adapter<Map<K, Set<V>>>(mapType).withNulls(withNulls)
}

inline fun <reified T> String.fromJson(): T {
  return getJsonAdapter<T>().fromJson(this)!!
}

inline fun <reified T> T.toJson(withNulls: Boolean = false): String {
  return getJsonAdapter<T>(withNulls).toJson(this)
}

inline fun <reified T> String.fromJsonList(withNulls: Boolean = false): List<T> {
  return getJsonListAdapter<T>(withNulls).fromJson(this)!!
}

inline fun <reified T> String.fromJsonSet(withNulls: Boolean = false): Set<T> {
  return getJsonSetAdapter<T>(withNulls).fromJson(this)!!
}

inline fun <reified K, reified V> String.fromJsonMap(): Map<K, V> {
  val mapType = newParameterizedType(Map::class.java, K::class.java, V::class.java)
  val adapter = MOSHI.adapter<Map<K, V>>(mapType)
  return adapter.fromJson(this)!!
}

inline fun <reified K, reified V> BufferedSource.fromJsonMapList(): Map<K, List<V>> {
  val listType = newParameterizedType(List::class.java, V::class.java)
  val mapType = newParameterizedType(Map::class.java, K::class.java, listType)
  val adapter = MOSHI.adapter<Map<K, List<V>>>(mapType)

  return adapter.fromJson(this)!!
}

inline fun <reified K, reified V> BufferedSource.fromJsonMapSet(): Map<K, Set<V>> {
  return getJsonMapSetAdapter<K, V>().fromJson(this)!!
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified K, reified V> File.bufferWriteJsonMap(
  set: Map<K, V>,
  withNulls: Boolean = false,
  indent: String = noJsonIndent
) {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonMapAdapter<K, V>(withNulls).indent(indent).toJson(writer, set)
  }
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified K, reified V> File.bufferWriteJsonMapSet(set: Map<K, Set<V>>, indent: String = noJsonIndent) {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonMapSetAdapter<K, V>().indent(indent).toJson(writer, set)
  }
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified T> File.bufferWriteJsonList(set: List<T>, indent: String = noJsonIndent) {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonListAdapter<T>().indent(indent).toJson(writer, set)
  }
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified T> File.bufferWriteJsonSet(set: Set<T>, indent: String = noJsonIndent) {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonSetAdapter<T>().indent(indent).toJson(writer, set)
  }
}

// TODO(tsr): gzip. centralize, update docs
inline fun <reified T> File.gzipCompress(set: Set<T>, indent: String = noJsonIndent) {
  JsonWriter.of(GzipSink(sink()).buffer()).use { writer ->
    getJsonSetAdapter<T>().indent(indent).toJson(writer, set)
  }
}

// TODO(tsr): gzip. centralize, update docs
inline fun <reified T> RegularFileProperty.gzipDecompress(): Set<T> {
  return GzipSource(get().asFile.source()).buffer().use { source ->
    getJsonSetAdapter<T>().fromJson(source)!!
  }
}

/**
 * Buffers writes of the object to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param obj The object to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified T> File.bufferWriteJson(obj: T, indent: String = noJsonIndent) {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonAdapter<T>().indent(indent).toJson(writer, obj)
  }
}

inline fun <reified A, reified B> File.bufferWriteParameterizedJson(
  parameterizedData: A,
  indent: String = noJsonIndent
) {
  JsonWriter.of(sink().buffer()).use { writer ->
    MOSHI.adapter<A>(newParameterizedType(A::class.java, B::class.java))
      .indent(indent)
      .toJson(writer, parameterizedData)
  }
}

@Suppress("unused")
internal class TypeAdapters {

  @ToJson fun fileToJson(file: File): String = file.absolutePath
  @FromJson fun fileFromJson(absolutePath: String): File = File(absolutePath)
}

@Suppress("unused")
internal class GraphAdapter {

  @ToJson fun graphViewToJson(graphView: DependencyGraphView): GraphViewJson {
    return GraphViewJson(
      sourceKind = graphView.sourceKind,
      configurationName = graphView.configurationName,
      graphJson = GraphJson(
        nodes = graphView.graph.nodes().toSortedSet(),
        edges = graphView.graph.edges().asSequence().map { pair ->
          pair.nodeU() to pair.nodeV()
        }.toSortedSet(),
      ),
    )
  }

  @FromJson fun jsonToGraphView(json: GraphViewJson): DependencyGraphView {
    return DependencyGraphView(
      sourceKind = json.sourceKind,
      configurationName = json.configurationName,
      graph = jsonToGraph(json),
    )
  }

  @ToJson fun graphToJson(graph: Graph<Coordinates>): GraphJson {
    return GraphJson(
      nodes = graph.nodes().toSortedSet(),
      edges = graph.edges().asSequence().map { pair ->
        pair.nodeU() to pair.nodeV()
      }.toSortedSet(),
    )
  }

  @FromJson fun jsonToGraph(json: GraphJson): Graph<Coordinates> {
    val graphBuilder = DependencyGraphView.newGraphBuilder()
    json.nodes.forEach { graphBuilder.addNode(it) }
    json.edges.forEach { (source, target) -> graphBuilder.putEdge(source, target) }

    return graphBuilder.build()
  }

  private fun jsonToGraph(json: GraphViewJson): Graph<Coordinates> {
    val graphBuilder = DependencyGraphView.newGraphBuilder()
    json.graphJson.nodes.forEach { graphBuilder.addNode(it) }
    json.graphJson.edges.forEach { (source, target) -> graphBuilder.putEdge(source, target) }

    return graphBuilder.build()
  }

  private infix fun Coordinates.to(target: Coordinates) = EdgeJson(this, target)

  @JsonClass(generateAdapter = false)
  internal data class GraphContainer(val graph: Graph<Coordinates>)

  @JsonClass(generateAdapter = false)
  internal data class GraphViewJson(
    val sourceKind: SourceKind,
    val configurationName: String,
    val graphJson: GraphJson,
  )

  @JsonClass(generateAdapter = false)
  internal data class GraphJson(
    val nodes: Set<Coordinates>,
    val edges: Set<EdgeJson>,
  )

  @JsonClass(generateAdapter = false)
  internal data class EdgeJson(val source: Coordinates, val target: Coordinates) : Comparable<EdgeJson> {
    // TODO(tsr): similar code in GraphWriter
    override fun compareTo(other: EdgeJson): Int {
      return compareBy(EdgeJson::source)
        .thenComparing(EdgeJson::target)
        .compare(this, other)
    }
  }
}
