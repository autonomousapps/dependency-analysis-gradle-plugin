@file:Suppress("UnstableApiUsage")

package com.autonomousapps.internal.graph

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.graph.ElementOrder
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import com.google.common.graph.ImmutableGraph

internal operator fun <T> Graph<T>.plus(other: Graph<T>): Graph<T> where T : Any {
  val builder = newGraphBuilder<T>()

  nodes().forEach { builder.addNode(it) }
  edges().forEach { edge -> builder.putEdge(edge.source(), edge.target()) }
  other.nodes().forEach { builder.addNode(it) }
  other.edges().forEach { edge -> builder.putEdge(edge.source(), edge.target()) }

  return builder.build()
}

/**
 * Flattens a graph by stripping out the
 * [GradleVariantIdentification][com.autonomousapps.model.GradleVariantIdentification], which essentially combines nodes
 * to different variants of a module into a single node. This simplifies reporting on certain scenarios where the module
 * itself is the unit of analysis, rather than the variant.
 */
internal fun Graph<Coordinates>.stripVariants(buildPath: String): Graph<Coordinates> {
  val builder = newGraphBuilder<Coordinates>()

  nodes().forEach { builder.addNode(it.maybeProjectCoordinates(buildPath).flatten()) }
  edges().forEach { edge ->
    val source = edge.source().maybeProjectCoordinates(buildPath).flatten()
    val target = edge.target().maybeProjectCoordinates(buildPath).flatten()

    // In the un-flattened graphs, self-loops are sort of possible because nodes with different capabilities are
    // different.
    if (source != target) {
      builder.putEdge(source, target)
    }
  }

  return builder.build()
}

internal fun <T> newGraphBuilder(): ImmutableGraph.Builder<T> {
  return GraphBuilder.directed()
    .allowsSelfLoops(false)
    .incidentEdgeOrder(ElementOrder.stable<T>())
    .immutable()
}

/**
 * Might transform [this][Coordinates] into [ProjectCoordinates], if it is an [IncludedBuildCoordinates] that is from
 * "this" build (with buildPath == [buildPath]).
 */
internal fun Coordinates.maybeProjectCoordinates(buildPath: String): Coordinates {
  return if (this is IncludedBuildCoordinates && isForBuild(buildPath)) resolvedProject else this
}

private fun Coordinates.flatten(): Coordinates {
  return if (this is ProjectCoordinates) flatten() else this
}
