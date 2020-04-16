package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency

class EmptyFilter : DependencyFilter {
  override val predicate: (Dependency) -> Boolean = { true }
}