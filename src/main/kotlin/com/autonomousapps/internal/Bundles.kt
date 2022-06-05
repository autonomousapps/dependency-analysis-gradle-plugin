package com.autonomousapps.internal

import com.autonomousapps.extension.DependenciesHandler.SerializableBundles
import com.autonomousapps.internal.graph.Graphs.children
import com.autonomousapps.internal.graph.Graphs.reachableNodes
import com.autonomousapps.model.Advice
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.DependencyGraphView
import com.autonomousapps.model.ProjectCoordinates
import com.autonomousapps.model.declaration.Bucket
import com.autonomousapps.model.intermediates.Usage

/**
 * :proj
 * |
 * B -> unused, not declared, but top of graph (added by plugin)
 * |
 * C -> used as API, part of bundle with B. Should not be declared!
 */
internal class Bundles(private val dependencyUsages: Map<Coordinates, Set<Usage>>) {

  // a sort of adjacency-list structure
  private val parentKeyedBundle = mutableMapOf<Coordinates, MutableSet<Coordinates>>()

  // link child/transitive node to parent node (which is directly adjacent to root project node)
  private val parentPointers = mutableMapOf<Coordinates, Coordinates>()

  // if a set of rules has a primary identifier, use this to map advice to it
  private val primaryPointers = mutableMapOf<Coordinates, Coordinates>()

  operator fun set(parentNode: Coordinates, childNode: Coordinates) {
    // nb: parents point to themselves as well. This is what lets DoubleDeclarationsSpec pass.
    parentKeyedBundle.merge(parentNode, mutableSetOf(parentNode, childNode)) { acc, inc ->
      acc.apply { addAll(inc) }
    }
    parentPointers.putIfAbsent(parentNode, parentNode)
    parentPointers.putIfAbsent(childNode, parentNode)
  }

  fun setPrimary(primary: Coordinates, subordinate: Coordinates) {
    primaryPointers.putIfAbsent(subordinate, primary)
  }

  fun hasParentInBundle(coordinates: Coordinates): Boolean = parentPointers[coordinates] != null

  fun hasUsedChild(coordinates: Coordinates): Boolean {
    val children = parentKeyedBundle[coordinates] ?: return false
    return children.any { child ->
      dependencyUsages[child].orEmpty().any { it.bucket != Bucket.NONE }
    }
  }

  fun primary(addAdvice: Advice): Advice {
    check(addAdvice.isAdd()) { "Must be add-advice" }
    return primaryPointers[addAdvice.coordinates]?.let { primary ->
      addAdvice.copy(coordinates = primary)
    } ?: addAdvice
  }

  companion object {
    fun of(
      projectNode: ProjectCoordinates,
      dependencyGraph: Map<String, DependencyGraphView>,
      bundleRules: SerializableBundles,
      dependencyUsages: Map<Coordinates, Set<Usage>>,
      ignoreKtx: Boolean
    ): Bundles {
      val bundles = Bundles(dependencyUsages)

      // Handle bundles with primary entry points
      bundleRules.primaries.forEach { (name, primaryId) ->
        val regexes = bundleRules.rules[name]!!
        dependencyGraph.forEach { (_, view) ->
          view.graph.reachableNodes(projectNode)
            .find { it.identifier == primaryId }
            ?.let { primaryNode ->
              val reachableNodes = view.graph.reachableNodes(primaryNode)
              reachableNodes.filter { subordinateNode ->
                regexes.any { it.matches(subordinateNode.identifier) }
              }.forEach { subordinateNode ->
                bundles.setPrimary(primaryNode, subordinateNode)
              }
            }
        }
      }

      // Handle bundles that don't have a primary entry point
      dependencyGraph.forEach { (_, view) ->
        view.graph.children(projectNode).forEach { parentNode ->
          val rules = bundleRules.matchingBundles(parentNode)

          // handle user-supplied bundles
          if (rules.isNotEmpty()) {
            val reachableNodes = view.graph.reachableNodes(parentNode)
            rules.forEach { (_, regexes) ->
              reachableNodes.filter { childNode ->
                regexes.any { it.matches(childNode.identifier) }
              }.forEach { childNode ->
                bundles[parentNode] = childNode
              }
            }
          }

          // handle dynamic ktx bundles
          if (ignoreKtx) {
            if (parentNode.identifier.endsWith("-ktx")) {
              val baseId = parentNode.identifier.substringBeforeLast("-ktx")
              view.graph.children(parentNode).find { child ->
                child.identifier == baseId
              }?.let { bundles[parentNode] = it }
            }
          }

          // Implicit KMP bundles (inverse form compared to ktx)
          val jvmCandidate = parentNode.identifier + "-jvm"
          view.graph.children(parentNode)
            .find { it.identifier == jvmCandidate }
            ?.let { bundles[parentNode] = it }
        }
      }

      return bundles
    }
  }
}
