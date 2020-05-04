package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.Dependency

internal interface DependencyFilter {
  val predicate: (Dependency) -> Boolean
}
