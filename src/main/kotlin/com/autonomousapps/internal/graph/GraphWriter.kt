package com.autonomousapps.internal.graph

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.graph.Graph
import org.gradle.kotlin.dsl.support.appendReproducibleNewLine

// TODO move
@Suppress("UnstableApiUsage")
internal object GraphWriter {

  fun toDot(graph: Graph<Coordinates>) = buildString {
    val projectNodes = graph.nodes()
      .filterIsInstance<ProjectCoordinates>()
      .map { it.gav() }

    appendReproducibleNewLine("strict digraph DependencyGraph {")
    appendReproducibleNewLine("  ratio=0.6;")
    appendReproducibleNewLine("  node [shape=box];")

    // styling for project nodes
    if (projectNodes.isNotEmpty()) appendReproducibleNewLine()
    projectNodes.forEach {
      appendReproducibleNewLine("  \"$it\" [style=filled fillcolor=\"#008080\"];")
    }
    if (projectNodes.isNotEmpty()) appendReproducibleNewLine()

    // the graph itself
    graph.edges().forEach { edge ->
      val source = edge.nodeU()
      val target = edge.nodeV()
      val style =
        if (source is ProjectCoordinates && target is ProjectCoordinates) " [style=bold color=\"#FF6347\" weight=8]"
        else ""
      append("  \"${source.gav()}\" -> \"${target.gav()}\"$style;")
      append("\n")
    }
    append("}")
  }
}
