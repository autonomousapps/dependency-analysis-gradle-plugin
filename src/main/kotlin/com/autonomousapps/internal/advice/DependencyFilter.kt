package com.autonomousapps.internal.advice

import com.autonomousapps.advice.Dependency

internal interface DependencyFilter {
  val predicate: (Dependency) -> Boolean
}
