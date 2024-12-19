package com.autonomousapps.internal.graph.supers

import com.autonomousapps.internal.graph.newGraphBuilder
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.BinaryClassCapability
import com.autonomousapps.visitor.GraphViewVisitor
import com.google.common.graph.Graph

/** Builds a [`Graph<SuperNode>`][Graph]. */
@Suppress("UnstableApiUsage")
internal class SuperClassGraphBuilder {
  private val nodes = mutableMapOf<String, SuperNode>()
  private val edges = mutableMapOf<String, MutableSet<String>>()

  private fun putEdge(from: SuperNode, to: SuperNode) {
    putNode(from)
    putNode(to)

    edges.merge(from.className, mutableSetOf(to.className)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
  }

  private fun putNode(node: SuperNode) {
    nodes.merge(node.className, node) { acc, inc ->
      acc.apply { deps.addAll(inc.deps) }
    }
  }

  private fun graph(): Graph<SuperNode> {
    val graphBuilder = newGraphBuilder<SuperNode>()
    edges.forEach { edge ->
      val from = nodes[edge.key]!!
      edge.value.forEach {
        val to = nodes[it]!!
        graphBuilder.putEdge(from, to)
      }
    }

    return graphBuilder.build()
  }

  companion object {
    /** Builds a graph from child classes up through super classes and interfaces, up to java.lang.Object. */
    fun of(context: GraphViewVisitor.Context): Graph<SuperNode> {
      val builder = SuperClassGraphBuilder()

      context.dependencies.forEach { dep ->
        dep.findCapability<BinaryClassCapability>()?.let { capability ->
          capability.binaryClasses.map { bin ->
            val from = SuperNode(bin.className).apply {
              deps += dep.coordinates
            }
            builder.putNode(from)

            // edge from the child class to its super class, if it has one
            bin.superClassName?.let { superClassName ->
              val to = SuperNode(superClassName)
              builder.putEdge(from, to)
            }

            // edge from the child class to each of its interfaces, if it has any
            if (bin.interfaces.isNotEmpty()) {
              bin.interfaces.forEach { i ->
                val to = SuperNode(i)
                builder.putEdge(from, to)
              }
            }
          }
        }
      }

      return builder.graph()
    }
  }
}
