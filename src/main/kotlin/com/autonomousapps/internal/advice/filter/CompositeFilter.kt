package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.Dependency
import java.util.*

internal class CompositeFilter(
  private val filters: Collection<DependencyFilter> = emptyList()
) : DependencyFilter {

  fun copy(filter: DependencyFilter): CompositeFilter {
    val filterSet = LinkedList<DependencyFilter>()
    filterSet.addAll(filters)
    filterSet.add(filter)
    return CompositeFilter(filterSet)
  }

  // nb if list is empty, filters.all {} will return true, which is a good thing
  override val predicate: (Dependency) -> Boolean = { dependency ->
    filters.all { it.predicate(dependency) }
  }
}
