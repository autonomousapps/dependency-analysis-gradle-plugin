// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
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
@Suppress("UnstableApiUsage")
internal class Bundles private constructor(private val dependencyUsages: Map<Coordinates, Set<Usage>>) {

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
      val preferredCoordinatesNotation =
        if (primary is IncludedBuildCoordinates && addAdvice.coordinates is ProjectCoordinates) {
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
      ignoreKtx: Boolean,
    ): Bundles {
      val bundles = Bundles(dependencyUsages)

      // Handle bundles with primary entry points
      bundleRules.primaries.forEach { (name, primaryId) ->
        val regexes = bundleRules.rules[name]!!
        dependencyGraph.values.forEach { view ->
          val projectNode = view.graph.nodes().find { it.identifier == projectPath }!!
          view.graph.reachableNodes(projectNode)
            .find { child -> child.matches(primaryId) }
            ?.let { primaryNode ->
              val subordinateNodes = view.graph.reachableNodes(primaryNode)
              subordinateNodes.filter { subordinateNode ->
                regexes.any { subordinateNode.matches(it) }
              }.forEach { subordinateNode ->
                bundles.setPrimary(primaryNode, subordinateNode)
              }
            }
        }
      }

      // Handle bundles that don't have a primary entry point
      dependencyGraph.values.forEach { view ->
        // Find the node that represents the current project, which always exists in the graph
        val projectNode = view.graph.nodes().find { it.identifier == projectPath }!!
        view.graph.children(projectNode).forEach { parentNode ->
          val rules = bundleRules.matchingBundles(parentNode)

          // handle user-supplied bundles
          if (rules.isNotEmpty()) {
            val reachableNodes = view.graph.reachableNodes(parentNode)
            rules.values.forEach { regexes ->
              reachableNodes.filter { childNode ->
                regexes.any { childNode.matches(it) }
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
                child.matches(baseId)
              }?.let { bundles[parentNode] = it }
            }
          }

          fun implicitKmpBundleFor(target: String) {
            val candidate = "${parentNode.identifier}-${target}"
            view.graph.children(parentNode)
              .find { it.matches(candidate) }
              ?.let { bundles[parentNode] = it }
          }

          // Implicit KMP bundles for JVM and Android (inverse form compared to ktx)
          implicitKmpBundleFor("jvm")
          implicitKmpBundleFor("android")
        }

        fun implicitKmpPrimaryFor(target: String) {
          val suffix = "-$target"
          view.graph.nodes()
            .filter { it.identifier.endsWith(suffix) }
            .mapNotNull { candidate ->
              val kmpIdentifier = candidate.identifier.substringBeforeLast(suffix)
              val kmp = candidate.copy(
                kmpIdentifier,
                GradleVariantIdentification(setOf(kmpIdentifier), candidate.gradleVariantIdentification.attributes)
              )
              if (view.graph.hasEdgeConnecting(kmp, candidate)) {
                kmp to candidate
              } else {
                null
              }
            }
            .forEach { (kmp, candidate) ->
              bundles.setPrimary(kmp, candidate)
            }
        }

        // Find implicit KMP JVM and Android bundles buried in the graph
        implicitKmpPrimaryFor("jvm")
        implicitKmpPrimaryFor("android")
      }

      return bundles
    }
  }
}

internal fun Coordinates.matches(primaryId: String): Boolean {
  return identifier == primaryId || this is IncludedBuildCoordinates && resolvedProject.identifier == primaryId
}

internal fun Coordinates.matches(regex: Regex): Boolean {
  return regex.matches(identifier) || this is IncludedBuildCoordinates && regex.matches(resolvedProject.identifier)
}
