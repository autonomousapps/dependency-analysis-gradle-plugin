package com.autonomousapps.internal

import com.autonomousapps.extension.DependenciesHandler.SerializableBundles
import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.model.*
import com.autonomousapps.model.Coordinates.Companion.copy
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
  fun findParentInBundle(coordinates: Coordinates): Coordinates? = parentPointers[coordinates]

  fun hasUsedChild(coordinates: Coordinates): Boolean {
    val children = parentKeyedBundle[coordinates] ?: return false
    return children.any { child ->
      dependencyUsages[child].orEmpty().any { it.bucket != Bucket.NONE }
    }
  }

  fun findUsedChild(coordinates: Coordinates): Coordinates? {
    val children = parentKeyedBundle[coordinates] ?: return null
    return children.find { child ->
      dependencyUsages[child].orEmpty().any { it.bucket != Bucket.NONE }
    }
  }

  fun maybePrimary(addAdvice: Advice, originalCoordinates: Coordinates): Advice {
    check(addAdvice.isAdd()) { "Must be add-advice" }
    return primaryPointers[originalCoordinates]?.let { primary ->
      val preferredCoordinatesNotation = if (primary is IncludedBuildCoordinates && addAdvice.coordinates is ProjectCoordinates) {
        primary.resolvedProject
      } else {
        primary
      }
      addAdvice.copy(coordinates = preferredCoordinatesNotation.withoutDefaultCapability())
    } ?: addAdvice
  }

  companion object {
    fun of(
      projectPath: String,
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
          val projectNode = view.graph.nodes().find { it.identifier == projectPath }!!
          view.graph.reachableNodes(projectNode)
            .find { coordinatesOrPathEquals(it, primaryId) }
            ?.let { primaryNode ->
              val reachableNodes = view.graph.reachableNodes(primaryNode)
              reachableNodes.filter { subordinateNode ->
                regexes.any { coordinatesOrPathMatch(subordinateNode, it) }
              }.forEach { subordinateNode ->
                bundles.setPrimary(primaryNode, subordinateNode)
              }
            }
        }
      }

      // Handle bundles that don't have a primary entry point
      dependencyGraph.forEach { (_, view) ->
        val projectNode = view.graph.nodes().find { it.identifier == projectPath }!!
        view.graph.children(projectNode).forEach { parentNode ->
          val rules = bundleRules.matchingBundles(parentNode)

          // handle user-supplied bundles
          if (rules.isNotEmpty()) {
            val reachableNodes = view.graph.reachableNodes(parentNode)
            rules.forEach { (_, regexes) ->
              reachableNodes.filter { childNode ->
                regexes.any { coordinatesOrPathMatch(childNode, it) }
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
                coordinatesOrPathEquals(child, baseId)
              }?.let { bundles[parentNode] = it }
            }
          }

          // Implicit KMP bundles (inverse form compared to ktx)
          val jvmCandidate = parentNode.identifier + "-jvm"
          view.graph.children(parentNode)
            .find { coordinatesOrPathEquals(it, jvmCandidate) }
            ?.let { bundles[parentNode] = it }
        }

        // Find implicit KMP bundles buried in the graph
        @Suppress("UnstableApiUsage") // guava
        view.graph.nodes()
          .filter { it.identifier.endsWith("-jvm") }
          .mapNotNull { jvm ->
            val kmpIdentifier = jvm.identifier.substringBeforeLast("-jvm")
            val kmp = jvm.copy(kmpIdentifier, GradleVariantIdentification(setOf(kmpIdentifier), jvm.gradleVariantIdentification.attributes))
            if (view.graph.hasEdgeConnecting(kmp, jvm)) {
              kmp to jvm
            } else {
              null
            }
          }
          .forEach { (kmp, jvm) ->
            bundles.setPrimary(kmp, jvm)
          }
      }

      return bundles
    }

    private fun coordinatesOrPathEquals(coordinates: Coordinates, primaryId: String) =
      coordinates.identifier == primaryId || (coordinates is IncludedBuildCoordinates) && coordinates.resolvedProject.identifier == primaryId

    private fun coordinatesOrPathMatch(coordinates: Coordinates, regex: Regex) =
      regex.matches(coordinates.identifier) || (coordinates is IncludedBuildCoordinates) && regex.matches(coordinates.resolvedProject.identifier)

  }
}
