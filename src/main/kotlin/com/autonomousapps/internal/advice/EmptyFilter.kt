package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Dependency

class EmptyFilter : DependencyFilter {
  override val predicate: (Dependency) -> Boolean = { true }
}