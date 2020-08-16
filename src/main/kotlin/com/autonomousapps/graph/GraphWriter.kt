package com.autonomousapps.graph

internal object GraphWriter {

  fun toDot(graph: DependencyGraph) = buildString {
    append("strict digraph DependencyGraph {\n")
    graph.edges().forEach { edge ->
      val (from, to) = edge.nodeIds()
      append("  \"$from\" -> \"$to\";")
      append("\n")
    }
    append("}")
  }

  fun toDot(path: Iterable<Edge>) = buildString {
    append("strict digraph DependencyGraph {\n")
    path.forEach { edge ->
      val (from, to) = edge.nodeIds()
      append("  \"$from\" -> \"$to\";")
      append("\n")
    }
    append("}")
  }

  fun toDot(graph: DependencyGraph, path: Iterable<Edge>) = buildString {
    val importantNodes = path.flatMap {
      it.nodeIds().toList()
    }

    append("strict digraph DependencyGraph {\n")
    importantNodes.forEach {
      append("\n  \"$it\" [style=filled fillcolor=\"#008080\"];\n")
    }
    append("\n")

    graph.edges().forEach { edge ->
      val style =
        if (path.contains(edge)) " [style=bold color=\"#FF6347\" weight=8]"
        else ""

      val (from, to) = edge.nodeIds()
      append("  \"$from\" -> \"$to\"$style;")
      append("\n")
    }
    append("}")
  }
}
