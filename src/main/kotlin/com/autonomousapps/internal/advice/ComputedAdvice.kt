package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.internal.AnnotationProcessor
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.VariantDependency
import com.autonomousapps.internal.advice.filter.FilterSpecBuilder
import com.autonomousapps.internal.utils.*

internal class ComputedAdvice(
  unusedComponents: Set<ComponentWithTransitives>,
  undeclaredApiDependencies: Set<TransitiveDependency>,
  undeclaredImplDependencies: Set<TransitiveDependency>,
  changeToApi: Set<VariantDependency>,
  changeToImpl: Set<VariantDependency>,
  compileOnlyCandidates: Set<Component>,
  unusedProcs: Set<AnnotationProcessor>,
  filterSpecBuilder: FilterSpecBuilder
) {

  private val filterSpec = filterSpecBuilder.build()

  val filterRemove = filterSpec.filterRemove
  val filterAdd = filterSpec.filterAdd
  val filterChange = filterSpec.filterChange
  val filterCompileOnly = filterSpec.filterCompileOnly
  val filterUnusedProcsAdvice = filterSpec.filterUnusedProcs

  val addToApiAdvice: Set<Advice> = undeclaredApiDependencies
    .filterToOrderedSet(filterSpec.addAdviceFilter)
    .mapToOrderedSet { Advice.ofAdd(it, "api") }

  val addToImplAdvice: Set<Advice> = undeclaredImplDependencies
    .filterToOrderedSet(filterSpec.addAdviceFilter)
    // Remove any dependencies that are in the add-to-api set
    .filterToOrderedSet { trans ->
      addToApiAdvice.none { api ->
        api.dependency == trans.dependency
      }
    }
    .mapToOrderedSet { Advice.ofAdd(it, "implementation") }

  val removeAdvice: Set<Advice> = unusedComponents
    .filterToOrderedSet(filterSpec.removeAdviceFilter)
    .mapToOrderedSet { Advice.ofRemove(it) }

  val changeToApiAdvice: Set<Advice> = changeToApi
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .flatMapToOrderedSet { variantDependency ->
      variantDependency.computeToConfigurations("api").map {
        Advice.ofChange(variantDependency, toConfiguration = it)
      }
    }
    // Filter out those already on the correct configuration
    .filterToOrderedSet { it.dependency.configurationName != it.toConfiguration }

  val changeToImplAdvice: Set<Advice> = changeToImpl
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .flatMapToOrderedSet { variantDependency ->
      variantDependency.computeToConfigurations("implementation").map {
        Advice.ofChange(variantDependency, toConfiguration = it)
      }
    }
    // Filter out those already on the correct configuration
    .filterToOrderedSet { it.dependency.configurationName != it.toConfiguration }

  /**
   * Given a [VariantDependency] and an expected [conf], determine the set of configurations it
   * ought to be on. For example, if the `VariantDependency` has variants `["main", ...]`, then it
   * should be on the "main" configuration, such as "api" or "implementation". If the variant set
   * doesn't contain "main" (rather ["debug", "release", ...], then it's variant-only, and it should
   * be on the configurations "debugApi" and "releaseApi", etc. If the variant set is empty and
   * the dependency is already on a variant of the given `conf`, assume this is correct. If the
   * variant set is empty and the dependency is _not_ on a variant of the `conf`, this is incorrect
   * and we return the main configuration, or "api" in this example.
   */
  private fun VariantDependency.computeToConfigurations(conf: String): Set<String> = when {
    // ["main"] -> ["api"]
    variants.contains("main") -> setOf(conf)
    // ["debug", "release"] -> ["debugApi", "releaseApi"]
    variants.isNotEmpty() -> variants.mapToSet { "${it}${conf.capitalizeSafely()}" }
    // [] -> ["debugApi"]
    dependency.configurationName?.endsWith(conf, ignoreCase = true) == true -> {
      setOf(dependency.configurationName)
    }
    // [] -> ["api"]
    else -> setOf(conf)
  }

  val compileOnlyAdvice: Set<Advice> = compileOnlyCandidates
    // We want to exclude transitives here. In other words, don't advise people to declare
    // used-transitive components.
    .filterToOrderedSet { !it.isTransitive }
    .mapToOrderedSet { it.dependency }
    // Don't advise changing dependencies that are already compileOnly
    .filterToOrderedSet { it.configurationName?.endsWith("compileOnly") == false }
    .filterToOrderedSet(filterSpec.compileOnlyAdviceFilter)
    // TODO be variant-aware
    .mapToOrderedSet { Advice.ofChange(it, "compileOnly") }

  val unusedProcsAdvice: Set<Advice> = unusedProcs
    .filterToOrderedSet(filterSpec.unusedProcsAdviceFilter)
    .mapToOrderedSet { Advice.ofRemove(it.dependency) }

  fun getAdvices(): Set<Advice> {
    val advices = sortedSetOf<Advice>()

    /*
     * Doing this all in a "functional" way would result in many many intermediate sets being
     * created, needlessly.
     */

    addToApiAdvice.forEach { advices.add(it) }
    addToImplAdvice.forEach { advices.add(it) }
    removeAdvice.forEach { advices.add(it) }
    changeToApiAdvice.forEach { advices.add(it) }
    changeToImplAdvice.forEach { advices.add(it) }
    compileOnlyAdvice.forEach { advices.add(it) }
    unusedProcsAdvice.forEach { advices.add(it) }

    return advices
  }
}