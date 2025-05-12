// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.source.SourceKind
import com.google.common.graph.Graph
import com.google.common.graph.ImmutableGraph

/**
 * This is a metadata view of a [configurationName] classpath. It expresses the relationship between the dependencies in
 * the classpath, as a dependency resolution engine (such as Gradle's) understands it. [name] is the Android variant, or
 * the JVM [SourceSet][org.gradle.api.tasks.SourceSet], that it represents.
 */
@Suppress("UnstableApiUsage") // Guava graphs
internal class DependencyGraphView(
  val sourceKind: SourceKind,
  /** E.g. `compileClasspath` or `debugRuntimeClasspath`. */
  val configurationName: String,
  /** The dependency DAG. */
  internal val graph: Graph<Coordinates>,
) {

  /** The variant (Android) or source set (JVM) name. */
  val name: String = "${sourceKind.name},${sourceKind.kind}"

  val nodes: Set<Coordinates> by unsafeLazy { graph.nodes() }

  companion object {
    internal fun newGraphBuilder(): ImmutableGraph.Builder<Coordinates> {
      return com.autonomousapps.internal.graph.newGraphBuilder()
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DependencyGraphView

    if (name != other.name) return false
    if (configurationName != other.configurationName) return false
    if (graph != other.graph) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + configurationName.hashCode()
    result = 31 * result + graph.hashCode()
    return result
  }
}
