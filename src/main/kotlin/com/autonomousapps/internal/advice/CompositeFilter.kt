package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Dependency

internal class CompositeFilter(
  private val filters: Collection<DependencyFilter> = emptyList()
) : DependencyFilter {

  // nb if list is empty, filters.all {} will return true, which is a good thing
  override val predicate: (Dependency) -> Boolean = { dependency ->
    filters.all { it.predicate(dependency) }
  }
}
