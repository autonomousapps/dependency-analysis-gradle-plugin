package com.autonomousapps.internal.advice

import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.filterNoneMatchingSorted
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet

/**
 * Classes of advice:
 * 1. Declared dependencies which are not used: remove
 * 2. Undeclared (transitive) dependencies are are used: add
 * 3. `implementation` dependencies incorrectly declared `api`
 * 4. `api` dependencies incorrectly declared `implementation`.
 * 5. `compileOnly` dependencies incorrectly declared as `api` or `implementation`.
 *
 * [ignoreKtx] is applied to the remove-advice to filter (or not) ktx-related advice; supplied by
 * user. If true, we don't suggest removing an "unused" `-ktx` dependency iff any of its
 * dependencies are used (directly or transitively).
 */
internal class Advisor(
  private val allComponents: List<Component>,
  private val unusedDirectComponents: List<UnusedDirectComponent>,
  private val usedTransitiveComponents: List<TransitiveComponent>,
  private val abiDeps: List<Dependency>,
  private val allDeclaredDeps: List<Dependency>,
  private val ignoreKtx: Boolean = false
) {

  /**
   * Computes all the advice in one pass.
   */
  fun compute(filterSpecBuilder: FilterSpecBuilder = FilterSpecBuilder()): ComputedAdvice {
    val compileOnlyCandidates = computeCompileOnlyCandidates()
    val unusedDependencies = computeUnusedDependencies(compileOnlyCandidates)
    val undeclaredApiDependencies = computeUndeclaredApiDependencies(compileOnlyCandidates)
    val undeclaredImplDependencies = computeUndeclaredImplDependencies(undeclaredApiDependencies, compileOnlyCandidates)
    val changeToApi = computeApiDepsWronglyDeclared(compileOnlyCandidates)
    val changeToImpl = computeImplDepsWronglyDeclared(compileOnlyCandidates, unusedDependencies)

    // update filterSpecBuilder with ktxFilter
    if (ignoreKtx) {
      val ktxFilter = KtxFilter(allComponents, unusedDirectComponents, usedTransitiveComponents, unusedDependencies)
      filterSpecBuilder.addToUniversalFilter(ktxFilter)
    }

    return ComputedAdvice(
      compileOnlyCandidates = compileOnlyCandidates,
      unusedDependencies = unusedDependencies,
      undeclaredApiDependencies = undeclaredApiDependencies,
      undeclaredImplDependencies = undeclaredImplDependencies,
      changeToApi = changeToApi,
      changeToImpl = changeToImpl,
      filterSpecBuilder = filterSpecBuilder
    )
  }

  /**
   * A [Component] is a compileOnly candidate iff:
   * 1. It has already been determined to be based on analysis done in [AnalyzedJar]; OR
   * 2. It is currently on a variant of the `compileOnly` configuration (here we assume users know what they're doing).
   */
  private fun computeCompileOnlyCandidates(): Set<Component> {
    return allComponents
      .filterToOrderedSet {
        it.isCompileOnlyAnnotations || it.dependency.configurationName?.endsWith("compileOnly", ignoreCase = true) == true
      }
  }

  /**
   * A [Dependency] is unused (and should be removed) iff:
   * 1. It is in the st of [unusedDirectComponents] AND
   * 2. It is not also in the set [compileOnlyCandidates] (we do not suggest removing such candidates, even if they
   *    appear unused).
   */
  private fun computeUnusedDependencies(compileOnlyCandidates: Set<Component>): Set<Dependency> {
    return unusedDirectComponents
      .mapToOrderedSet { it.dependency }
      .stripCompileOnly(compileOnlyCandidates)
  }

  /**
   * A [Dependency] is an undeclared `api` dependency (and should be declared as such) iff:
   * 1. It is part of the project's ABI AND
   * 2. It was not declared (it's [configurationName][Dependency.configurationName] is `null`) AND
   * 3. It was not declared to be `compileOnly` (here we assume users know what they're doing).
   */
  private fun computeUndeclaredApiDependencies(compileOnlyCandidates: Set<Component>): Set<Dependency> {
    return abiDeps
      .filterToOrderedSet { it.configurationName == null }
      .stripCompileOnly(compileOnlyCandidates)
  }

  /**
   * A [Dependency] is an undeclared `implementation` dependency (and should be declared as such) iff:
   * 1. It is in the set of [usedTransitiveComponents] AND
   * 2. It is not an undeclared `api` dependency (see [computeUndeclaredApiDependencies]) AND
   * 3. It is not a `compileOnly` candidate (see [computeCompileOnlyCandidates]).
   */
  private fun computeUndeclaredImplDependencies(
    undeclaredApiDeps: Set<Dependency>,
    compileOnlyCandidates: Set<Component>
  ): Set<Dependency> {
    return usedTransitiveComponents
      .mapToOrderedSet { it.dependency }
      // Exclude any transitives which will be api dependencies
      .filterNoneMatchingSorted(undeclaredApiDeps)
      .stripCompileOnly(compileOnlyCandidates)
  }

  /**
   * A [Dependency] is a "wrongly declared" api dep (and should be changed) iff:
   * 1. It is not transitive ([configuration][Dependency.configurationName] must be non-null).
   * 2. It _should_ be on `api`, but is on something else AND
   * 3. It is not a `compileOnly` candidate (see [computeCompileOnlyCandidates]).
   */
  private fun computeApiDepsWronglyDeclared(compileOnlyCandidates: Set<Component>): Set<Dependency> {
    return abiDeps
      // Filter out those with a null configuration, as they are handled elsewhere
      .filterToOrderedSet { it.configurationName != null }
      // Filter out those with an "api" configuration, as they're already correct.
      .filterToOrderedSet { !it.configurationName!!.endsWith("api", ignoreCase = true) }
      .stripCompileOnly(compileOnlyCandidates)
  }

  /**
   * A [Dependency] is a "wrongly declared" impl dep (and should be changed) iff:
   * 1. It is not transitive ([configuration][Dependency.configurationName] must be non-null); AND
   * 2. It is used; AND
   * 2. It is not part of the project's ABI; AND
   * 3. It is not a `compileOnly` candidate (see [computeCompileOnlyCandidates]).
   */
  private fun computeImplDepsWronglyDeclared(
    compileOnlyCandidates: Set<Component>, unusedDependencies: Set<Dependency>
  ): Set<Dependency> {
    return allDeclaredDeps
      // Filter out those with a null configuration, as they are handled elsewhere
      .filterToOrderedSet { it.configurationName != null }
      // Filter out those with an "implementation" configuration, as they're already correct.
      .filterToOrderedSet { !it.configurationName!!.endsWith("implementation", ignoreCase = true) }
      // Filter out those that are unused
      .filterNoneMatchingSorted(unusedDependencies)
      // Filter out those that actually should be api
      .filterNoneMatchingSorted(abiDeps)
      .stripCompileOnly(compileOnlyCandidates)
  }

  private fun Iterable<Dependency>.stripCompileOnly(compileOnlyCandidates: Set<Component>): Set<Dependency> {
    return filterToOrderedSet { dep ->
      compileOnlyCandidates.none { compileOnly ->
        dep == compileOnly.dependency
      }
    }
  }
}
