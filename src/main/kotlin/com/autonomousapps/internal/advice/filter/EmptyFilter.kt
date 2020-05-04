package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.HasDependency

internal object EmptyFilter : DependencyFilter {
  override val predicate: (HasDependency) -> Boolean = { true }
}