@file:JvmName("MoshiUtils")

package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.declaration.Variant
import com.google.common.graph.Graph
import com.squareup.moshi.*
import com.squareup.moshi.Types.newParameterizedType
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.zacsweers.moshix.sealed.reflect.MoshiSealedJsonAdapterFactory
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File

val MOSHI: Moshi by lazy {
  Moshi.Builder()
    .add(GraphViewAdapter())
    .add(MoshiSealedJsonAdapterFactory())
    .add(TypeAdapters())
    .addLast(KotlinJsonAdapterFactory())
    .build()
}

inline fun <reified T> getJsonAdapter(withNulls: Boolean = false): JsonAdapter<T> {
  val adapter = MOSHI.adapter(T::class.java)
  return if (withNulls) {
    adapter.serializeNulls()
  } else {
    adapter
  }
}

inline fun <reified T> getJsonListAdapter(withNulls: Boolean = false): JsonAdapter<List<T>> {
  val type = newParameterizedType(List::class.java, T::class.java)
  val adapter = MOSHI.adapter<List<T>>(type)
  return if (withNulls) {
    adapter.serializeNulls()
  } else {
    adapter
  }
}

inline fun <reified T> getJsonSetAdapter(withNulls: Boolean = false): JsonAdapter<Set<T>> {
  val type = newParameterizedType(Set::class.java, T::class.java)
  val adapter = MOSHI.adapter<Set<T>>(type)
  return if (withNulls) {
    adapter.serializeNulls()
  } else {
    adapter
  }
}

inline fun <reified K, reified V> getJsonMapAdapter(withNulls: Boolean = false): JsonAdapter<Map<K, V>> {
  val type = newParameterizedType(Map::class.java, K::class.java, V::class.java)
  val adapter = MOSHI.adapter<Map<K, V>>(type)
  return if (withNulls) {
    adapter.serializeNulls()
  } else {
    adapter
  }
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
  val setType = newParameterizedType(Set::class.java, V::class.java)
  val mapType = newParameterizedType(Map::class.java, K::class.java, setType)
  val adapter = MOSHI.adapter<Map<K, Set<V>>>(mapType)

  return adapter.fromJson(this)!!
}

inline fun <reified T> T.toPrettyString(withNulls: Boolean = false): String {
  return getJsonAdapter<T>(withNulls).indent("  ").toJson(this)
}

inline fun <reified K, reified V> Map<K, V>.toPrettyString(withNulls: Boolean = false): String {
  return getJsonMapAdapter<K, V>(withNulls).indent("  ").toJson(this)
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified T> File.bufferWriteJsonSet(set: Set<T>, indent: String = "") {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonSetAdapter<T>().indent(indent).toJson(writer, set)
  }
}

/**
 * Buffers writes of the set to disk, using the indent to make the output human-readable.
 * By default, the output is compacted.
 *
 * @param set The set to write to file
 * @param indent The indent to control how the result is formatted
 */
inline fun <reified T> File.bufferWriteJson(set: T, indent: String = "") {
  JsonWriter.of(sink().buffer()).use { writer ->
    getJsonAdapter<T>().indent(indent).toJson(writer, set)
  }
}

@Suppress("unused")
internal class TypeAdapters {

  @ToJson fun fileToJson(file: File): String = file.absolutePath
  @FromJson fun fileFromJson(absolutePath: String): File = File(absolutePath)
}

@Suppress("unused", "UnstableApiUsage")
internal class GraphViewAdapter {

  @ToJson fun graphViewToJson(graphView: DependencyGraphView): GraphViewJson {
    return GraphViewJson(
      variant = graphView.variant,
      configurationName = graphView.configurationName,
      nodes = graphView.graph.nodes(),
      edges = graphView.graph.edges().asSequence().map { pair ->
        pair.nodeU() to pair.nodeV()
      }.toSet()
    )
  }

  @FromJson fun jsonToGraphView(json: GraphViewJson): DependencyGraphView {
    return DependencyGraphView(
      variant = json.variant,
      configurationName = json.configurationName,
      graph = jsonToGraph(json)
    )
  }

  private fun jsonToGraph(json: GraphViewJson): Graph<Coordinates> {
    val graphBuilder = DependencyGraphView.newGraphBuilder()
    json.nodes.forEach { graphBuilder.addNode(it) }
    json.edges.forEach { (source, target) -> graphBuilder.putEdge(source, target) }

    return graphBuilder.build()
  }

  @JsonClass(generateAdapter = false)
  internal data class GraphViewJson(
    val variant: Variant,
    val configurationName: String,
    val nodes: Set<Coordinates>,
    val edges: Set<EdgeJson>
  )

  @JsonClass(generateAdapter = false)
  internal data class EdgeJson(val source: Coordinates, val target: Coordinates)

  private infix fun Coordinates.to(target: Coordinates) = EdgeJson(this, target)
}
