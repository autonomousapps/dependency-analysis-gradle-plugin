package com.autonomousapps.internal.advice

import com.autonomousapps.internal.Dependency

internal interface DependencyFilter {
  val predicate: (Dependency) -> Boolean
}
