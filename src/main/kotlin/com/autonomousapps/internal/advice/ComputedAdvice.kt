package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.Dependency
import com.autonomousapps.advice.TransitiveDependency
import com.autonomousapps.internal.AnnotationProcessor
import com.autonomousapps.internal.Component
import com.autonomousapps.internal.advice.filter.FilterSpecBuilder
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet

internal class ComputedAdvice(
  unusedComponents: Set<ComponentWithTransitives>,
  undeclaredApiDependencies: Set<TransitiveDependency>,
  undeclaredImplDependencies: Set<TransitiveDependency>,
  changeToApi: Set<Dependency>,
  changeToImpl: Set<Dependency>,
  compileOnlyCandidates: Set<Component>,
  unusedProcs: Set<AnnotationProcessor>,
  filterSpecBuilder: FilterSpecBuilder
) {

  private val filterSpec = filterSpecBuilder.build()

  val compileOnlyDependencies: Set<Dependency> = compileOnlyCandidates
    // We want to exclude transitives here. In other words, don't advise people to declare
    // used-transitive components.
    .filterToOrderedSet { !it.isTransitive }
    .mapToOrderedSet { it.dependency }
    // Don't advise changing dependencies that are already compileOnly
    .filterToOrderedSet { it.configurationName?.endsWith("compileOnly") == false }
    .filterToOrderedSet(filterSpec.compileOnlyAdviceFilter)

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
    .mapToOrderedSet { Advice.ofAdd(it, "implementation") }

  val removeAdvice: Set<Advice> = unusedComponents
    .filterToOrderedSet(filterSpec.removeAdviceFilter)
    .mapToOrderedSet { Advice.ofRemove(it) }

  val changeToApiAdvice: Set<Advice> = changeToApi
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .mapToOrderedSet { Advice.ofChange(it, toConfiguration = "api") }

  val changeToImplAdvice: Set<Advice> = changeToImpl
    .filterToOrderedSet(filterSpec.changeAdviceFilter)
    .mapToOrderedSet { Advice.ofChange(it, toConfiguration = "implementation") }

  val compileOnlyAdvice: Set<Advice> = compileOnlyDependencies
    .filterToOrderedSet(filterSpec.compileOnlyAdviceFilter)
    // TODO be variant-aware
    .mapToOrderedSet { Advice.ofCompileOnly(it, "compileOnly") }

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