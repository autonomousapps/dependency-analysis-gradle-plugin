package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.ComponentWithTransitives
import com.autonomousapps.advice.HasDependency
import com.autonomousapps.advice.TransitiveDependency

internal class FacadeFilter(private val facadeGroups: Set<String>) : DependencyFilter {

  companion object {
    val EMPTY = FacadeFilter(emptySet())
  }

  override val predicate: (HasDependency) -> Boolean = {
    if (facadeGroups.isEmpty()) {
      // Exit early if we have no facade groups
      true
    } else {
      val isFacade = when (it) {
        is TransitiveDependency -> it.isFacade
        is ComponentWithTransitives -> it.isFacade
        else -> error("This filter expects a TransitiveDependency or a ComponentWithTransitives")
      }
      // recall that if this returns true, then the dependency is _kept_ in the advice. So, here we
      // return true only if this dependency is not a facade or not a facade we care about.
      !isFacade || !facadeGroups.contains(it.dependency.group)
    }
  }
}
