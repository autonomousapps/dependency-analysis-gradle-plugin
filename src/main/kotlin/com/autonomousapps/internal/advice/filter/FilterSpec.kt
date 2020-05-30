package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.HasDependency
import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.Ignore
import com.autonomousapps.extension.Warn

internal class FilterSpecBuilder {
  // Behaviors
  var anyBehavior: Behavior = Warn()
  var unusedDependenciesBehavior: Behavior = Warn()
  var usedTransitivesBehavior: Behavior = Warn()
  var incorrectConfigurationsBehavior: Behavior = Warn()
  var compileOnlyBehavior: Behavior = Warn()
  var unusedProcsBehavior: Behavior = Warn()

  // Filters
  var universalFilter: CompositeFilter = CompositeFilter()
  var facadeFilter: FacadeFilter = FacadeFilter.EMPTY

  fun addToUniversalFilter(filter: DependencyFilter) {
    universalFilter = universalFilter.copy(filter)
  }

  fun build(): FilterSpec {
    return FilterSpec(
      anyBehavior = anyBehavior,
      unusedDependenciesBehavior = unusedDependenciesBehavior,
      usedTransitivesBehavior = usedTransitivesBehavior,
      incorrectConfigurationsBehavior = incorrectConfigurationsBehavior,
      compileOnlyBehavior = compileOnlyBehavior,
      unusedProcsBehavior = unusedProcsBehavior,
      universalFilter = universalFilter,
      facadeFilter = facadeFilter
    )
  }
}

/**
 * A container for the various filters, to be applied to the final advice:
 * - [universalFilter] applied to all advice; built into plugin.
 * - [facadeFilter] applied to all advice; built into plugin, but also modifiable by user.
 * - [anyBehavior] applied to all advice; supplied by user.
 * - [unusedDependenciesBehavior] applied to remove-advice; supplied by user.
 * - [usedTransitivesBehavior] applied to add-dependencies advice; supplied by user.
 * - [incorrectConfigurationsBehavior] applied change-advice; supplied by user.
 * - [compileOnlyBehavior] applied to compileOnly-advice; supplied by user.
 * - [unusedProcsBehavior] applied to unusedProcs-advice; supplied by user.
 */
internal class FilterSpec(
  private val universalFilter: CompositeFilter = CompositeFilter(),
  private val facadeFilter: FacadeFilter,
  private val anyBehavior: Behavior = Warn(),
  private val unusedDependenciesBehavior: Behavior = Warn(),
  private val usedTransitivesBehavior: Behavior = Warn(),
  private val incorrectConfigurationsBehavior: Behavior = Warn(),
  private val compileOnlyBehavior: Behavior = Warn(),
  private val unusedProcsBehavior: Behavior = Warn()
) {

  private val shouldIgnoreAll = anyBehavior is Ignore
  val filterRemove = shouldIgnoreAll || unusedDependenciesBehavior is Ignore
  val filterAdd = shouldIgnoreAll || usedTransitivesBehavior is Ignore
  val filterChange = shouldIgnoreAll || incorrectConfigurationsBehavior is Ignore
  val filterCompileOnly = shouldIgnoreAll || compileOnlyBehavior is Ignore
  val filterUnusedProcs = shouldIgnoreAll || unusedProcsBehavior is Ignore

  val removeAdviceFilter: (HasDependency) -> Boolean = { hasDependency ->
    val dependency = hasDependency.dependency
    if (anyBehavior is Ignore || unusedDependenciesBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all unused dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(unusedDependenciesBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, or is a facade we care about, ignore it
      universalFilter.predicate(dependency) && facadeFilter.predicate(hasDependency)
    }
  }

  val addAdviceFilter: (HasDependency) -> Boolean = { hasDependency ->
    val dependency = hasDependency.dependency
    if (anyBehavior is Ignore || usedTransitivesBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all undeclared transitive dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(usedTransitivesBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, or is a facade we care about, ignore it
      universalFilter.predicate(dependency) && facadeFilter.predicate(hasDependency)
    }
  }

  val changeAdviceFilter: (HasDependency) -> Boolean = { hasDependency ->
    val dependency = hasDependency.dependency
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

  val unusedProcsAdviceFilter: (HasDependency) -> Boolean = { hasDependency ->
    val dependency = hasDependency.dependency
    if (anyBehavior is Ignore || unusedProcsBehavior is Ignore) {
      // If we're ignoring everything or just ignoring all unused procs dependencies, then do that
      false
    } else if (anyBehavior.filter.plus(unusedProcsBehavior.filter).contains(dependency.identifier)) {
      // If we're ignoring some specific dependencies, then do that
      false
    } else {
      // If the dependency is universally ignored, do that
      universalFilter.predicate(dependency)
    }
  }
}
