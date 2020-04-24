package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Component
import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.TransitiveComponent
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.internal.utils.filterToSet
import com.autonomousapps.internal.utils.mapToSet

/**
 * "KTX Dependencies" are those which
 * 1. Have an [identifier][Dependency.identifier] that ends with "-ktx"
 * 2. are nominally unused by which contribute transitive dependencies which _are_ used. We only
 *    care about them if [FilterSpec.ignoreKtx] is `true`.
 *
 * tl;dr: an empty set means don't change the advice, while a non-empty set means we're going to remove some things
 * from the final advice.
 *
 * @return the set of dependencies which must be filtered from the set returned by
 * [computeUnusedDependencies][com.autonomousapps.internal.advice.Advisor.computeUnusedDependencies],
 * to respect the user's preference to "ignore ktx dependencies."
 */
internal class KtxFilter(
    private val allComponents: Set<Component>,
    private val unusedDirectComponents: Set<ComponentWithTransitives>,
    private val usedTransitiveComponents: Set<TransitiveComponent>,
    private val unusedDependencies: Set<Dependency>
) : DependencyFilter {

  private val filterSet: Set<Dependency>

  init {
    val ktxTransitives = computeKtxTransitives()
    val ktxDirects = computeKtxDirects(ktxTransitives)
    val allKtxCandidates = computeAllKtxCandidates(ktxTransitives, ktxDirects)
    val usedDeps = computeUsedDependencies()
    val usedKtxDeps = computeUsedKtxDeps(allKtxCandidates, usedDeps)
    filterSet = computeFilterSet(usedKtxDeps)
  }

  override val predicate: (Dependency) -> Boolean = { dependency ->
    !filterSet.contains(dependency)
  }

  /**
   * These are the transitive dependencies of our directly-declared ktx dependencies.
   */
  private fun computeKtxTransitives(): MutableMap<Dependency, Set<Dependency>> {
    return unusedDirectComponents
      // Get the unused ktx dependencies
      .filterToSet { it.dependency.identifier.endsWith("-ktx") }
      // Get all the transitive dependencies of the -ktx dependencies
      .associateTo(mutableMapOf()) { it.dependency to it.usedTransitiveDependencies }
  }

  /**
   * These are the directly declared dependencies which also happen to be transitively required by our ktx deps.
   */
  private fun computeKtxDirects(ktxTransitives: Map<Dependency, Set<Dependency>>): MutableMap<Dependency, Set<Dependency>> {
    return allComponents
      .filterToSet { !it.isTransitive }
      .mapToSet { it.dependency }
      .mapToSet { direct ->
        val parents = ktxTransitives.keys.filter { key ->
          ktxTransitives.getValue(key).contains(direct)
        }
        parents.associateWith { setOf(direct) }
      }.fold(mutableMapOf()) { acc, map ->
        acc.apply { putAll(map) }
      }
  }

  /**
   * Basically a union of the two maps we've created.
   */
  private fun computeAllKtxCandidates(
      ktxTransitives: Map<Dependency, Set<Dependency>>,
      ktxDirects: Map<Dependency, Set<Dependency>>
  ): MutableMap<Dependency, MutableSet<Dependency>> {
    val ktxMap = mutableMapOf<Dependency, MutableSet<Dependency>>()
    for (element in ktxTransitives) {
      ktxMap[element.key] = element.value.toMutableSet()
    }
    for (element in ktxDirects) {
      val set = ktxMap.getOrPut(element.key) { mutableSetOf() }
      set.addAll(element.value)
    }
    return ktxMap
  }

  /**
   * All of the used dependencies.
   */
  private fun computeUsedDependencies(): Set<Dependency> {
    return allComponents
      .filterToSet { !it.isTransitive }
      .mapToSet { it.dependency }
      // We only care about those that are used
      .filterToSet { directDependency ->
        unusedDependencies.none {
          it == directDependency
        }
      } + usedTransitiveComponents.mapToSet { it.dependency }
  }

  /**
   * Filter the union to contain only those elements whose transitives are used.
   */
  private fun computeUsedKtxDeps(
      ktxCandidates: Map<Dependency, Set<Dependency>>,
      usedDependencies: Set<Dependency>
  ): Map<Dependency, Set<Dependency>> {
    return ktxCandidates.filter { (_, children) ->
      usedDependencies.any { children.contains(it) }
    }.filter { it.value.isNotEmpty() }
  }

  /**
   * All dependencies that are either a parent or child of the unioned-and-filtered map of ktx candidates.
   */
  private fun computeFilterSet(usedKtxDeps: Map<Dependency, Set<Dependency>>): Set<Dependency> {
    return allComponents.mapToSet { it.dependency }
      .filterToSet { candidate ->
        usedKtxDeps[candidate] != null || usedKtxDeps.values.any { it.contains(candidate) }
      }
  }
}
