package com.autonomousapps.advice

import com.autonomousapps.graph.DependencyGraph
import com.autonomousapps.internal.utils.*

/**
 * Q: What are the potential ripples from following the advice for project D? (given by --id)
 *
 * Compile classpath of A and E (which are disjoint!). The solid lines indicate "api" dependencies
 * (so they get exposed transitively). Dotted lines indicate "implementation" dependencies (which
 * are not exposed). Graph flows from top to bottom.
 *
 * Note that, in particular, F cannot be reached via A -> C -> E -> F, because the E -> F connection
 * is `implementation`.
 *
 * ```
 * A:
 * A
 * | \
 * B  C
 * |  |
 * D  E
 * |  ┊
 * F ┄╛
 *
 * E:
 * E
 * |
 * F
 * ```
 *
 * A and B use F directly, but do not declare dependencies on it. They access it transitively via
 * A -> B -> D -> F. D declares an api dependency on F, but only needs to declare an implementation
 * dependency: this is called a "downgrade." If we follow this advice, the compile classpath graphs
 * for A and B will change.
 *
 * ```
 * A
 * | \
 * B  C
 * |  |
 * D  E
 * ┊  ┊
 * F ┄╛
 * ```
 *
 * The chain from A -> B -> ... -> F has been broken. Therefore there are ripples in A and B from
 * the downgrade of F in D.
 *
 * 1. User says they want to clean up D, so they request the ripples for that project:
 *    `gw :ripples --id :D`
 * 2. From the `List<ComprehensiveAdvice>` (aka "buildHealth"), pull out all the
 *    [ComprehensiveAdvice.dependencyAdvice] for project ":D" => { D downgrades F }.
 * 3. Compute the dependents of D => { A, B }.
 * 4. For each dependent of D ({ A, B }), see if there is any upgrade advice in the buildHealth =>
 *    { A upgrades F, B upgrades F }.
 * 5. For each graph in the set of upgrades ({ G_A, G_B }), remove the edge representing the
 *    downgrade ({ D -> F}), creating G_A' and G_B', and then compute if the proposed upgrade (F in
 *    each case) can be reached from the root node (A and B for G_A' and G_B', respectively).
 *
 *    If the upgrade cannot be reached, that is considered a ripple. Each ripple associates a
 *    downgrade-advice with an upgrade-advice.
 */
internal class RippleDetector(
  /** The project we want to clean. */
  private val queryProject: String,
  /** A mapping of project-path to dependency graph anchored on that project. */
  private val projectGraphProvider: (String) -> DependencyGraph,
  /**
   * The full dependency graph for the entire build. Not fully accurate, as it implies transitivity
   * that isn't actually present from the perspective of the compile classpath of any given node.
   */
  private val fullGraph: DependencyGraph,
  /** The comprehensive advice for the entire build. */
  private val buildHealth: Set<ComprehensiveAdvice>
) {

  val ripples: Set<Ripple>

  init {
    assert(queryProject.startsWith(":"))
    ripples = compute()
  }

  private fun compute(): Set<Ripple> {
    // 2. Downgrades for queryProject
    val downgrades = buildHealth.find { it.projectPath == queryProject }
      ?.dependencyAdvice
      ?.filterToSet { it.isDowngrade() }
      ?: error("Cannot find $queryProject in buildHealth, which should be impossible")

    // 3. Compute POTENTIAL dependents for queryProject
    val dependentsGraph = fullGraph.reversed()
    val dependentsGraphForProject = dependentsGraph.subgraph(queryProject)
    val dependentsForQueryProject = dependentsGraphForProject.nodes().mapToSet { it.identifier }

    // 4. For each potential dependent of queryProject, get upgrade-advice
    val upgrades = buildHealth.filterToSet { compAdvice ->
      dependentsForQueryProject.contains(compAdvice.projectPath)
    }.flatMap { compAdvice ->
      compAdvice.dependencyAdvice
        .filterToSet { it.isAdd() }
        .mapToSet { compAdvice.projectPath to mutableSetOf(it) }
    }.mergedMap()

    // 5. For each subgraph in set of upgrade-keys (which are projects), remove edge representing
    // the downgrade, creating new, shortened subgraphs. Then, compute if the proposed upgrade can
    // be reached from the root node in those shortened subgraphs
    return upgrades
      .map { upgrade -> upgrade to projectGraphProvider(upgrade.key) }
      .flatMapToSet { (upgrade, subgraph) ->
        downgrades.flatMapToSet { downgrade ->
          upgrade.value.mapNotNullToSet { upgradeAdvice ->
            val hasPath = subgraph.removeEdge(queryProject, downgrade.dependency.identifier)
              .subgraph(upgrade.key)
              .hasNode(upgradeAdvice.dependency.identifier)

            if (!hasPath) {
              Ripple(
                sourceProject = queryProject,
                impactedProject = upgrade.key,
                downgrade = downgrade,
                upgrade = upgradeAdvice
              )
            } else {
              null
            }
          }
        }
      }
  }
}

internal data class Ripple(
  val sourceProject: String,
  val impactedProject: String,
  val downgrade: Advice,
  val upgrade: Advice
)

/**
 * If this is advice to remove or downgrade an api-like dependency.
 */
private fun Advice.isDowngrade(): Boolean {
  return (isRemove() || isChange() || isCompileOnly())
    && dependency.configurationName?.endsWith("api", ignoreCase = true) == true
}
