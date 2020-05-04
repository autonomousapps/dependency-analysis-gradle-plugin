package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.Dependency
import com.autonomousapps.internal.advice.filter.DependencyFilter

class EmptyFilter : DependencyFilter {
  override val predicate: (Dependency) -> Boolean = { true }
}