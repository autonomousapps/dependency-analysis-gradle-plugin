// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.ProjectType
import com.autonomousapps.extension.DependenciesHandler.SerializableBundles
import com.autonomousapps.graph.Graphs.children
import com.autonomousapps.graph.Graphs.reachableNodes
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.model.*
import com.autonomousapps.model.Coordinates.Companion.copy
import com.autonomousapps.model.internal.DependencyGraphView
import com.autonomousapps.model.internal.declaration.Bucket
import com.autonomousapps.model.internal.declaration.ConfigurationNames
import com.autonomousapps.model.internal.declaration.Declaration
import com.autonomousapps.model.internal.intermediates.Usage

/**
 * :proj
 * |
 * B -> unused, not declared, but top of graph (added by plugin)
 * |
 * C -> used as API, part of bundle with B. Should not be declared!
 */
@Suppress("UnstableApiUsage")
internal class Bundles private constructor(
  private val dependencyUsages: Map<Coordinates, Set<Usage>>,
  private val declarations: Set<Declaration>,
  private val configurationNames: ConfigurationNames,
) {

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

  /**
   * Requirements for calling this method:
   * 1. [hasParentInBundle] has already been called and it returns true. Otherwise this method will throw.
   * 2. [addAdvice] is add-advice. Otherwise this method will throw.
   *
   * Can return either [addAdvice] exactly, or a change-advice. Will do the latter when the following is true:
   * 1. [originalCoordinates] is in a bundle with a declared parent.
   * 2. That declared parent is declared on another source set (currently either `commonMain` or `commonTest`) in a
   *    Kotlin Multiplatform (KMP) project.
   * 3. The parent declaration is implementation-scoped and [addAdvice] is api-scoped (that is, we'd like to upgrade).
   *
   * In this case, we suggest changing the parent declaration from `commonMainImplementation` or
   * `commonTestImplementation` to `commonMainApi` or `commonTestApi`, respectively.
   *
   * We do this because KMP is a special case where we know a priori that the commonX source sets are upstream from
   * target-specific source sets. We're being very conservative by only targeting the `implementation` -> `api` upgrade;
   * that is, the goal is to provide _safe_ advice even more than maximally-correct advice. Part of the problem here is
   * we're in an intermediate state where we only support JVM targets: we can't know how the other targets use the
   * common dependencies.
   *
   * @see [maybePrimary]
   */
  fun maybeParent(addAdvice: Advice, originalCoordinates: Coordinates): Advice {
    check(addAdvice.isAdd()) { "Must be add-advice" }

    val parent = findParentInBundle(originalCoordinates)
      ?: error("No parent for $originalCoordinates. Check 'hasParentInBundle()' before calling this method.")
    val parentCoordinates = preferredCoordinates(parent, addAdvice)

    val preferredBucket = Bucket.of(addAdvice.toConfiguration!!, configurationNames)

    // Get the source set name for the addAdvice. E.g., "jvmMain".
    val adviceSourceSetName = DependencyScope.sourceSetName(addAdvice.toConfiguration)

    // Find all declarations for this dependency. E.g., ["commonMainImplementation", "jvmMainImplementation"].
    val parentDeclarations = declarations.filterToOrderedSet { decl -> decl.identifier == parentCoordinates.identifier }

    // Pick the "highest" one (api > implementation > everything else)
    val declarationSelector: (Declaration) -> Int = { declaration ->
      when (declaration.bucket(configurationNames)) {
        Bucket.API -> 10
        Bucket.IMPL -> 1
        else -> -1
      }
    }

    // Find all declarations for the advice source set. E.g., ["jvmMainImplementation", "jvmMainApi"]. It's been known
    // to happen.
    var parentDeclaration = parentDeclarations
      .filter { declaration -> DependencyScope.sourceSetName(declaration.configurationName) == adviceSourceSetName }
      // Pick the "highest" one (api > implementation > everything else)
      // May be null (there may not be a declaration in the same source set).
      .maxByOrNull(declarationSelector)

    // If there are no declarations within the same source set, look at other source sets (that may be related).
    if (parentDeclaration == null) {
      parentDeclaration = parentDeclarations
        .filter { declaration -> DependencyScope.sourceSetName(declaration.configurationName) != adviceSourceSetName }
        // Pick the "highest" one (api > implementation > everything else)
        // Won't be null (we know there's a declaration _somewhere_).
        .maxBy(declarationSelector)
    }

    val parentBucket = parentDeclaration.bucket(configurationNames)

    // Only change the advice if it's from implementation -> api. We don't change compileOnly or runtimeOnly advice.
    return if (preferredBucket == Bucket.API && parentBucket == Bucket.IMPL) {
      // TODO(tsr): there's a bug that probably impacts all project types, but I've only just noticed while working on KMP.
      val toConfiguration = if (configurationNames.projectType == ProjectType.KMP) {
        when (parentDeclaration.configurationName) {
          // Handle the `commonX` cases
          "commonMainImplementation" -> "commonMainApi"
          "commonTestImplementation" -> "commonTestApi"

          // This means the parent is in the same source set
          else -> addAdvice.toConfiguration
        }
      } else {
        // TODO(tsr): not a KMP project. What can we say?
        null
      }

      if (toConfiguration != null) {
        Advice.ofChange(
          coordinates = parentCoordinates.withoutDefaultCapability(),
          fromConfiguration = parentDeclaration.configurationName,
          toConfiguration = toConfiguration,
        )
      } else {
        addAdvice
      }
    } else {
      addAdvice
    }
  }

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
      val preferredCoordinatesNotation = preferredCoordinates(primary, addAdvice)
      addAdvice.copy(coordinates = preferredCoordinatesNotation.withoutDefaultCapability())
    } ?: addAdvice
  }

  private fun preferredCoordinates(coordinates: Coordinates, advice: Advice): Coordinates {
    return if (coordinates is IncludedBuildCoordinates && advice.coordinates is ProjectCoordinates) {
      coordinates.resolvedProject
    } else {
      coordinates
    }
  }

  companion object {
    fun of(
      projectPath: String,
      dependencyGraph: Map<String, DependencyGraphView>,
      bundleRules: SerializableBundles,
      dependencyUsages: Map<Coordinates, Set<Usage>>,
      declarations: Set<Declaration>,
      configurationNames: ConfigurationNames,
      ignoreKtx: Boolean,
    ): Bundles {
      val bundles = Bundles(dependencyUsages, declarations, configurationNames)

      // Handle bundles with primary entry points
      bundleRules.primaries.forEach { (name, primaryId) ->
        val regexes = bundleRules.rules[name]!!
        dependencyGraph.values.forEach { view ->
          val projectNode = view.graph.nodes().find { node -> node.identifier == projectPath }!!
          view.graph.reachableNodes(projectNode)
            .find { child -> child.matches(primaryId) }
            ?.let { primaryNode ->
              val subordinateNodes = view.graph.reachableNodes(primaryNode)
              subordinateNodes
                .filter { subordinateNode ->
                  regexes.any { regex -> subordinateNode.matches(regex) }
                }
                .forEach { subordinateNode -> bundles.setPrimary(primaryNode, subordinateNode) }
            }
        }
      }

      // Handle bundles that don't have a primary entry point
      dependencyGraph.values.forEach { view ->
        // Find the node that represents the current project, which always exists in the graph
        val projectNode = view.graph.nodes().find { node -> node.identifier == projectPath }!!
        view.graph.children(projectNode).forEach { parentNode ->
          val rules = bundleRules.matchingBundles(parentNode)

          // handle user-supplied bundles
          if (rules.isNotEmpty()) {
            val reachableNodes = view.graph.reachableNodes(parentNode)
            rules.values
              .forEach { regexes ->
                reachableNodes
                  .filter { childNode -> regexes.any { regex -> childNode.matches(regex) } }
                  .forEach { childNode -> bundles[parentNode] = childNode }
              }
          }

          // handle dynamic ktx bundles
          if (ignoreKtx) {
            if (parentNode.identifier.endsWith("-ktx")) {
              val baseId = parentNode.identifier.substringBeforeLast("-ktx")
              view.graph.children(parentNode)
                .find { child -> child.matches(baseId) }
                ?.let { child -> bundles[parentNode] = child }
            }
          }

          fun implicitKmpBundleFor(target: String) {
            val candidate = "${parentNode.identifier}-${target}"
            view.graph.children(parentNode)
              .find { child -> child.matches(candidate) }
              ?.let { child -> bundles[parentNode] = child }
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
                GradleVariantIdentification(setOf(kmpIdentifier), candidate.gradleVariantIdentification.attributes),
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
