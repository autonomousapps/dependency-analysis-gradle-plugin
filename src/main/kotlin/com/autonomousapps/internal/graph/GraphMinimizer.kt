package com.autonomousapps.internal.graph

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.mapToMutableList

/**
 * Takes the original, strict build health (`List<ComprehensiveAdvice>`), and trims away add-advice
 * (undeclared yet used transitive dependencies) that are unnecessary for preserving compilation
 * correctness.
 */
internal class GraphMinimizer(
  private val buildHealth: List<ComprehensiveAdvice>,
  private val dependentsGraph: DependencyGraph,
  private val lazyDepGraph: (String) -> DependencyGraph
) {

  /**
   * The minimized build health, the result of transforming the strict build health by trimming away
   * unnecessary add-advices.
   */
  val minimalBuildHealth: List<ComprehensiveAdvice>

  /** map project producing the downgrades, to its downgrades */
  private val downgradeMap = mutableMapOf<String, MutableList<Downgrade>>()

  /** map project producing the upgrades, to its upgrade edges */
  private val upgradeMap = mutableMapOf<String, MutableList<Upgrade>>()

  /** Map of project to edges that should be added to or removed from that project's graph */
  private val affectedProjects = mutableMapOf<String, ImpactedProjectGraph>()

  /** Hypothetical graphs, the result of applying some strict advice */
  private val hypotheticalGraphs = mutableMapOf<String, DependencyGraph>()

  init {
    minimalBuildHealth = compute()
  }

  /** Compute minimized advice. */
  private fun compute(): List<ComprehensiveAdvice> {
    computeUpgradesAndDowngrades()
    computeAffectedProjects()
    computeHypotheticalGraphs()

    // Based on the hypothetical graphs, strip away advice that is not necessary for compile-time
    // correctness.
    return buildHealth.map { compAdvice ->
      val depAdvice = compAdvice.dependencyAdvice.filterToSet { advice ->
        if (!advice.isAdd()) {
          // Since we're only filtering out add-advice, if it's not that, keep it
          true
        } else if (advice.isToApiLike()) {
          // Keep add-advice that is "add to api"
          true
        } else {
          // Keep add-advice if it's necessary to keep the node in the compile graph, else ditch
          !hypotheticalGraphFor(compAdvice.projectPath).hasAdviceNode(advice)
        }
      }

      // The "minimized" advice
      compAdvice.copy(dependencyAdvice = depAdvice)
    }
  }

//  private fun computeImpacts(): Map<String, List<Impact>> {
//    return buildHealth.map { compAdvice ->
//      val projectPath = compAdvice.projectPath
//      projectPath to compAdvice.dependencyAdvice.mapNotNull { advice ->
//        when {
//          advice.isDowngrade() -> Downgrade(advice)
//          advice.isRemove() -> Downgrade(advice, true)
//          advice.isToApiLike() -> Upgrade(advice)
//          else -> null
//        }
//      }
//    }.mergedMap()
//  }

  /**
   * Given the original strict build health, computes a pair of maps from the source project to the
   * upgrade/downgrade advice those projects produce.
   */
  private fun computeUpgradesAndDowngrades() {
    buildHealth.forEach { compAdvice ->
      val projectPath = compAdvice.projectPath
      compAdvice.dependencyAdvice.forEach { advice ->
        when {
          advice.isDowngrade() -> downgradeMap.merge(
            projectPath,
            mutableListOf(Downgrade(advice))
          ) { acc, inc ->
            acc.apply { addAll(inc) }
          }
          advice.isRemove() -> downgradeMap.merge(
            projectPath,
            mutableListOf(Downgrade(advice, true))
          ) { acc, inc ->
            acc.apply { addAll(inc) }
          }
          advice.isToApiLike() -> upgradeMap.merge(
            projectPath,
            mutableListOf(Upgrade(advice))
          ) { acc, inc ->
            acc.apply { addAll(inc) }
          }
        }
      }
    }
  }

  /**
   * Computes the map of affected projects, from impacted project to graph-changes each would
   * experience (edge removals and additions).
   */
  private fun computeAffectedProjects() {
    // Apply downgrades to determine build-wide impact
    downgradeMap
      // Result of this is populating the map of affected projects
      .forEach { (projectPath, downgrades) ->
        val (selfDowngrades, transDowngrades) = downgrades.partition { it.isSelfDowngrade }

        // Handle transitive downgrades
        val transEdgeRemovals = transDowngrades.mapToMutableList {
          projectPath to it.id
        }
        dependentsGraph.subgraph(projectPath)
          .projectNodes()
          .forEach { projNode ->
            affectedProjects.merge(
              projNode, ImpactedProjectGraph(removals = transEdgeRemovals)
            ) { acc, inc ->
              acc.apply { removals.addAll(inc.removals) }
            }
          }

        // Handle self-downgrades
        val selfEdgeRemovals = selfDowngrades.mapToMutableList {
          projectPath to it.id
        }
        affectedProjects.merge(
          projectPath, ImpactedProjectGraph(removals = selfEdgeRemovals)
        ) { acc, inc ->
          acc.apply { removals.addAll(inc.removals) }
        }
      }

    // Apply upgrades to determine build-wide impact
    upgradeMap.forEach { (projectPath, upgrades) ->
      dependentsGraph.subgraph(projectPath)
        .projectNodes()
        .forEach { projNode ->
          val upgradeEdges = upgrades.mapToMutableList { projectPath to it.id }
          affectedProjects.merge(
            projNode, ImpactedProjectGraph(additions = upgradeEdges)
          ) { acc, inc ->
            acc.apply { additions.addAll(inc.additions) }
          }
        }
    }
  }

  /** Create hypothetical graphs, modified based on original strict advice */
  private fun computeHypotheticalGraphs() {
    affectedProjects.forEach { (projectPath, impactedGraph) ->
      val removableEdges = impactedGraph.removals
      val addableEdges = impactedGraph.additions
      val graph = hypotheticalGraphFor(projectPath).removeEdges(projectPath, removableEdges).apply {
        addEdges(addableEdges)
      }
      hypotheticalGraphs[projectPath] = graph
    }
  }

  private fun hypotheticalGraphFor(projectPath: String): DependencyGraph {
    return hypotheticalGraphs[projectPath] ?: getDependencyGraph(projectPath)
  }

  private fun getDependencyGraph(projectPath: String) = lazyDepGraph(projectPath)

  private fun DependencyGraph.hasAdviceNode(advice: Advice) = hasNode(advice.dependency.identifier)
}

private class ImpactedProjectGraph(
  val removals: MutableList<Pair<String, String>> = mutableListOf(),
  val additions: MutableList<Pair<String, String>> = mutableListOf()
)

private sealed class Impact(open val id: String)

private class Downgrade(
  override val id: String,
  val isSelfDowngrade: Boolean = false
) : Impact(id) {
  constructor(advice: Advice, isSelfDowngrade: Boolean = false) : this(
    advice.dependency.identifier,
    isSelfDowngrade
  )
}

private class Upgrade(override val id: String) : Impact(id) {
  constructor(advice: Advice) : this(advice.dependency.identifier)
}
