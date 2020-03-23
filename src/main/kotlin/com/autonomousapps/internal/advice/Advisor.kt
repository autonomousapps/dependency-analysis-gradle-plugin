package com.autonomousapps.internal.advice

import com.autonomousapps.Behavior
import com.autonomousapps.Ignore
import com.autonomousapps.Warn
import com.autonomousapps.internal.*
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import org.gradle.api.GradleException

/**
 * Classes of advice:
 * 1. Declared dependencies which are not used: remove
 * 2. Undeclared (transitive) dependencies are are used: add
 * 3. `implementation` dependencies incorrectly declared `api`
 * 4. `api` dependencies incorrectly declared `implementation`.
 * 5. `compileOnly` dependencies incorrectly declared as `api` or `implementation`.
 */
internal class Advisor(
  private val allComponents: List<Component>,
  private val unusedDirectComponents: List<UnusedDirectComponent>,
  private val usedTransitiveComponents: List<TransitiveComponent>,
  private val abiDeps: List<Dependency>,
  private val allDeclaredDeps: List<Dependency>
) {

  /**
   * Computes all the advice in one pass.
   */
  fun compute(filterSpec: FilterSpec = FilterSpec(
    universalFilter = CompositeFilter(listOf(DataBindingFilter(), ViewBindingFilter()))
  )): ComputedAdvice {
    val compileOnlyCandidates = computeCompileOnlyCandidates()
    val adviceUnused = computeUnusedDependencies(compileOnlyCandidates)
    val undeclaredApiDependencies = computeUndeclaredApiDependencies(compileOnlyCandidates)
    val undeclaredImplDependencies = computeUndeclaredImplDependencies(undeclaredApiDependencies, compileOnlyCandidates)
    val changeToApi = computeApiDepsWronglyDeclared(compileOnlyCandidates)
    val changeToImpl = computeImplDepsWronglyDeclared(compileOnlyCandidates)

    return ComputedAdvice(
      compileOnlyCandidates = compileOnlyCandidates,
      unusedDependencies = adviceUnused,
      undeclaredApiDependencies = undeclaredApiDependencies,
      undeclaredImplDependencies = undeclaredImplDependencies,
      changeToApi = changeToApi,
      changeToImpl = changeToImpl,
      filterSpec = filterSpec
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
      .filterToOrderedSet { dep ->
        compileOnlyCandidates.none { compileOnly ->
          dep == compileOnly.dependency
        }
      }
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
//      .filterToOrderedSet { dep ->
//        compileOnlyCandidates.none { compileOnly ->
//          dep == compileOnly.dependency
//        }
//      }
  }

  private fun Iterable<Dependency>.stripCompileOnly(compileOnlyCandidates: Set<Component>): Set<Dependency> {
    return filterToOrderedSet { dep ->
      compileOnlyCandidates.none { compileOnly ->
        dep == compileOnly.dependency
      }
    }
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
      .filterToOrderedSet { trans ->
        undeclaredApiDeps.none { api ->
          api == trans
        }
      }
      // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
      .filterToOrderedSet { dep ->
        compileOnlyCandidates.none { compileOnlyComponent ->
          compileOnlyComponent.dependency == dep
        }
      }
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
      // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
      .filterToOrderedSet { dep ->
        compileOnlyCandidates.none { compileOnlyComponent ->
          compileOnlyComponent.dependency == dep
        }
      }
  }

  /**
   * A [Dependency] is a "wrongly declared" impl dep (and should be changed) iff:
   * 1.
   */
  private fun computeImplDepsWronglyDeclared(
    compileOnlyCandidates: Set<Component>
  ): Set<Dependency> {
    return allDeclaredDeps
      // Filter out those with a null configuration, as they are handled elsewhere
      .filterToOrderedSet { it.configurationName != null }
      // Filter out those with an "implementation" configuration, as they're already correct.
      .filterToOrderedSet { !it.configurationName!!.endsWith("implementation", ignoreCase = true) }
      // Filter out those that actually should be api
      .filterToOrderedSet { dep ->
        abiDeps.none { abi ->
          abi == dep
        }
      }
      // Don't suggest adding a compileOnly candidate. They're handled elsewhere.
      .filterToOrderedSet { dep ->
        compileOnlyCandidates.none { compileOnlyComponent ->
          dep == compileOnlyComponent.dependency
        }
      }
  }
}

internal class ComputedAdvice(
  unusedDependencies: Set<Dependency>,
  undeclaredApiDependencies: Set<Dependency>,
  undeclaredImplDependencies: Set<Dependency>,
  changeToApi: Set<Dependency>,
  changeToImpl: Set<Dependency>,
  filterSpec: FilterSpec,
  compileOnlyCandidates: Set<Component>
) {

  val compileOnlyDependencies = compileOnlyCandidates
    // We want to exclude transitives here. In other words, don't advise people to declare used-transitive components.
    .filterToOrderedSet { !it.isTransitive }
    .mapToOrderedSet { it.dependency }
    .filterToOrderedSet(filterSpec.compileOnlyAdviceFilter)

  val filterRemove = filterSpec.filterRemove
  val filterAdd = filterSpec.filterAdd
  val filterChange = filterSpec.filterChange
  val filterCompileOnly = filterSpec.filterCompileOnly

  val addToApiAdvice: Set<Advice> = undeclaredApiDependencies
    .filterToOrderedSet(filterSpec.addAdviceFilter)
    .mapToOrderedSet { Advice.add(it, "api") }

  val addToImplAdvice: Set<Advice> = undeclaredImplDependencies
    .filterToOrderedSet(filterSpec.addAdviceFilter)
    .mapToOrderedSet { Advice.add(it, "implementation") }

  val removeAdvice: Set<Advice> = unusedDependencies
    .filterToOrderedSet(filterSpec.removeAdviceFilter)
    .mapToOrderedSet { Advice.remove(it) }

  val changeToApiAdvice: Set<Advice> = changeToApi
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .mapToOrderedSet { Advice.change(it, toConfiguration = "api") }

  val changeToImplAdvice: Set<Advice> = changeToImpl
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .mapToOrderedSet { Advice.change(it, toConfiguration = "implementation") }

  val compileOnlyAdvice: Set<Advice> = compileOnlyDependencies
    .filterToOrderedSet(filterSpec.compileOnlyAdviceFilter)
    // TODO be variant-aware
    .mapToOrderedSet { Advice.compileOnly(it, "compileOnly") }

  fun getAdvices(): Set<Advice> {
    val advices = sortedSetOf<Advice>()

    /*
     * Doing this all in a "functional" way would result in many many intermediate sets being created, needlessly.
     */

    addToApiAdvice.forEach { advices.add(it) }
    addToImplAdvice.forEach { advices.add(it) }
    removeAdvice.forEach { advices.add(it) }
    changeToApiAdvice.forEach { advices.add(it) }
    changeToImplAdvice.forEach { advices.add(it) }
    compileOnlyAdvice.forEach { advices.add(it) }

    return advices
  }

  fun advicePrinter(): AdvicePrinter = AdvicePrinter(this)
}

/**
 * Only concerned with human-readable advice meant to be printed to the console.
 */
internal class AdvicePrinter(private val computedAdvice: ComputedAdvice) {

  /**
   * Returns "add-advice" (or null if none) for printing to console.
   */
  fun getAddAdvice(): String? {
    val undeclaredApiDeps = computedAdvice.addToApiAdvice
    val undeclaredImplDeps = computedAdvice.addToImplAdvice

    if (undeclaredApiDeps.isEmpty() && undeclaredImplDeps.isEmpty()) {
      return null
    }

    val apiAdvice = undeclaredApiDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it.dependency)})"
    }
    val implAdvice = undeclaredImplDeps.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it.dependency)})"
    }

    return if (undeclaredApiDeps.isNotEmpty() && undeclaredImplDeps.isNotEmpty()) {
      "$apiAdvice\n$implAdvice"
    } else if (undeclaredApiDeps.isNotEmpty()) {
      apiAdvice
    } else if (undeclaredImplDeps.isNotEmpty()) {
      implAdvice
    } else {
      // One or the other list must be non-empty
      throw GradleException("Impossible")
    }
  }

  /**
   * Returns "remove-advice" (or null if none) for printing to console.
   */
  fun getRemoveAdvice(): String? {
    val unusedDependencies = computedAdvice.removeAdvice

    if (unusedDependencies.isEmpty()) {
      return null
    }

    return unusedDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      "${it.fromConfiguration}(${printableIdentifier(it.dependency)})"
    }
  }

  /**
   * Returns "change-advice" (or null if none) for printing to console.
   */
  fun getChangeAdvice(): String? {
    val changeToApi = computedAdvice.changeToApiAdvice
    val changeToImpl = computedAdvice.changeToImplAdvice

    if (changeToApi.isEmpty() && changeToImpl.isEmpty()) {
      return null
    }

    val apiAdvice = changeToApi.joinToString(prefix = "- ", separator = "\n- ") {
      "api(${printableIdentifier(it.dependency)}) // was ${it.fromConfiguration}"
    }
    val implAdvice = changeToImpl.joinToString(prefix = "- ", separator = "\n- ") {
      "implementation(${printableIdentifier(it.dependency)}) // was ${it.fromConfiguration}"
    }
    return if (changeToApi.isNotEmpty() && changeToImpl.isNotEmpty()) {
      "$apiAdvice\n$implAdvice"
    } else if (changeToApi.isNotEmpty()) {
      apiAdvice
    } else if (changeToImpl.isNotEmpty()) {
      implAdvice
    } else {
      // One or the other list must be non-empty
      throw GradleException("Impossible")
    }
  }

  /**
   * Returns "compileOnly-advice" (or null if none) for printing to console.
   */
  fun getCompileOnlyAdvice(): String? {
    val compileOnlyDependencies = computedAdvice.compileOnlyDependencies

    if (compileOnlyDependencies.isEmpty()) {
      return null
    }

    return compileOnlyDependencies.joinToString(prefix = "- ", separator = "\n- ") {
      // TODO be variant-aware
      "compileOnly(${printableIdentifier(it)}) // was ${it.configurationName}"
    }
  }

  private fun printableIdentifier(dependency: Dependency): String =
    if (dependency.identifier.startsWith(":")) {
      "project(\"${dependency.identifier}\")"
    } else {
      "\"${dependency.identifier}:${dependency.resolvedVersion}\""
    }
}

/**
 * A container for the various filters, to be applied to the final advice:
 * - [universalFilter] applied to all advice; built into plugin.
 * - [anyBehavior] applied to all advice; supplied by user.
 * - [unusedDependenciesBehavior] applied to remove-advice; supplied by user.
 * - [usedTransitivesBehavior] applied to add-dependencies advice; supplied by user.
 * - [incorrectConfigurationsBehavior] applied change-advice; supplied by user.
 * - [compileOnlyBehavior] applied to compileOnly-advice; supplied by user.
 */
internal class FilterSpec(
  private val universalFilter: DependencyFilter = CompositeFilter(listOf(DataBindingFilter(), ViewBindingFilter())),
  private val anyBehavior: Behavior = Warn(),
  private val unusedDependenciesBehavior: Behavior = Warn(),
  private val usedTransitivesBehavior: Behavior = Warn(),
  private val incorrectConfigurationsBehavior: Behavior = Warn(),
  private val compileOnlyBehavior: Behavior = Warn()
) {

  private val shouldIgnoreAll = anyBehavior is Ignore
  val filterRemove = shouldIgnoreAll || unusedDependenciesBehavior is Ignore
  val filterAdd = shouldIgnoreAll || usedTransitivesBehavior is Ignore
  val filterChange = shouldIgnoreAll || incorrectConfigurationsBehavior is Ignore
  val filterCompileOnly = shouldIgnoreAll || compileOnlyBehavior is Ignore

  val removeAdviceFilter: (Dependency) -> Boolean = { dependency ->
    if (anyBehavior is Ignore || unusedDependenciesBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all unused dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(unusedDependenciesBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, do that
      universalFilter.predicate(dependency)
    }
  }

  val addAdviceFilter: (Dependency) -> Boolean = { dependency ->
    if (anyBehavior is Ignore || usedTransitivesBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all undeclared transitive dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(usedTransitivesBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, do that
      universalFilter.predicate(dependency)
    }
  }

  val changeAdviceFilter: (Dependency) -> Boolean = { dependency ->
    if (anyBehavior is Ignore || incorrectConfigurationsBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all incorrectly-declared dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(incorrectConfigurationsBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, do that
      universalFilter.predicate(dependency)
    }
  }

  val compileOnlyAdviceFilter: (Dependency) -> Boolean = { dependency ->
    if (anyBehavior is Ignore || compileOnlyBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all compileOnly dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(compileOnlyBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, do that
      universalFilter.predicate(dependency)
    }
  }
}
