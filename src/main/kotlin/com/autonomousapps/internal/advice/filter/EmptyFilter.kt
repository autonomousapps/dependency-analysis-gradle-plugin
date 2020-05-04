package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.HasDependency

class EmptyFilter : DependencyFilter {
  override val predicate: (HasDependency) -> Boolean = { true }
}