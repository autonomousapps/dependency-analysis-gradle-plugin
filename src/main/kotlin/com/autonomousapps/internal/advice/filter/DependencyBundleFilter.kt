package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.HasDependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.graph.ShortestPath
import com.autonomousapps.internal.TransitiveComponent
import com.autonomousapps.internal.utils.mapToSet

internal class DependencyBundleFilter(
  private val bundles: Map<String, Set<Regex>>,
  private val compileGraph: DependencyGraph,
  private val testCompileGraph: DependencyGraph?,
  private val transitives: Set<TransitiveComponent>
) : DependencyFilter {

  companion object {
    val EMPTY = DependencyBundleFilter(emptyMap(), DependencyGraph(), null, emptySet())
  }

  private val compileParents by lazy(mode = LazyThreadSafetyMode.NONE) {
    compileGraph.adj(compileGraph.rootNode).mapToSet { it.to.identifier }
  }

  private val testCompileParents by lazy(mode = LazyThreadSafetyMode.NONE) {
    testCompileGraph?.let { graph ->
      graph.adj(graph.rootNode).mapToSet { it.to.identifier }
    }.orEmpty()
  }

  override val predicate: (HasDependency) -> Boolean = {
    if (bundles.isEmpty()) {
      // Exit early if we have no dependencies bundles
      true
    } else {
      val isBundle = when (it) {
        is TransitiveDependency -> computeTransitive(it)
        is ComponentWithTransitives -> computeDirect(it)
        else -> error("This filter expects a TransitiveDependency or a ComponentWithTransitives")
      }
      // Return true if the dependency doesn't match any of the dependency bundles the user has set
      !isBundle
    }
  }

  /**
   * Return true if [trans] has a parent in the same dependency bundle.
   */
  private fun computeTransitive(trans: TransitiveDependency): Boolean {
    // Find groups that `dep` is a member of
    val regexGroups = bundles.values
    val groups = regexGroups.filter { regexes ->
      regexes.any { regex ->
        regex.matches(trans.identifier)
      }
    }
    // Now look for parents in one of these groups, returning true if there is a match
    return groups.any { regexes ->
      regexes.any { regex ->
        isValidCompileBundle(regex, trans.identifier) ||
          isValidTestCompileBundle(regex, trans.identifier)
      }
    }
  }

  private fun isValidCompileBundle(regex: Regex, trans: String): Boolean {
    return compileParents.any { parent ->
      regex.matches(parent) && isValidBundle(compileGraph, parent, trans)
    }
  }

  private fun isValidTestCompileBundle(regex: Regex, trans: String): Boolean {
    return testCompileParents.any { parent ->
      regex.matches(parent) && isValidBundle(testCompileGraph!!, parent, trans)
    }
  }

  private fun isValidBundle(graph: DependencyGraph, source: String, trans: String): Boolean {
    return ShortestPath(graph, source).hasPathTo(trans)
  }

  /**
   * Return true if [direct] has a used transitive in the same dependency bundle.
   */
  private fun computeDirect(direct: ComponentWithTransitives): Boolean {
    // Find groups that `dep` is a member of
    val regexGroups = bundles.values
    val groups = regexGroups.filter { regexes ->
      regexes.any { regex ->
        regex.matches(direct.dependency.identifier)
      }
    }
    // Now look for transitives in one of these groups, returning true if there is a match
    return groups.any { regexes ->
      regexes.any { regex ->
        transitives.any { trans ->
          isValidDirectBundle(regex, direct.identifier, trans.identifier)
        }
      }
    }
  }

  private fun isValidDirectBundle(regex: Regex, direct: String, trans: String): Boolean {
    return regex.matches(trans) && hasRoute(direct, trans)
  }

  private fun hasRoute(direct: String, trans: String): Boolean {
    val hasCompileRoute =
      if (compileGraph.hasNode(direct)) ShortestPath(compileGraph, direct).hasPathTo(trans)
      else false

    if (hasCompileRoute) return true

    return if (testCompileGraph?.hasNode(direct) == true) {
      ShortestPath(testCompileGraph, direct).hasPathTo(trans)
    } else {
      false
    }
  }
}
