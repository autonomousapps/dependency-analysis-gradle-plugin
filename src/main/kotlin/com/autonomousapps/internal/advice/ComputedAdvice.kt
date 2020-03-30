package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Advice
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.Dependency
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet

internal class ComputedAdvice(
  unusedDependencies: Set<Dependency>,
  ktxDependencies: Set<Dependency>,
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
    .stripKtx(ktxDependencies)
    .mapToOrderedSet { Advice.add(it, "api") }

  val addToImplAdvice: Set<Advice> = undeclaredImplDependencies
    .filterToOrderedSet(filterSpec.addAdviceFilter)
    .stripKtx(ktxDependencies)
    .mapToOrderedSet { Advice.add(it, "implementation") }

  val removeAdvice: Set<Advice> = unusedDependencies
    .filterToOrderedSet(filterSpec.removeAdviceFilter)
    .stripKtx(ktxDependencies)
    .mapToOrderedSet { Advice.remove(it) }

  val changeToApiAdvice: Set<Advice> = changeToApi
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .stripKtx(ktxDependencies)
    .mapToOrderedSet { Advice.change(it, toConfiguration = "api") }

  val changeToImplAdvice: Set<Advice> = changeToImpl
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .stripKtx(ktxDependencies)
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

  private fun Iterable<Dependency>.stripKtx(ktxCandidates: Set<Dependency>): Set<Dependency> {
    return filterToOrderedSet { dep ->
      ktxCandidates.none { ktxCandidate ->
        dep == ktxCandidate
      }
    }
  }

  fun advicePrinter(): AdvicePrinter = AdvicePrinter(this)
}