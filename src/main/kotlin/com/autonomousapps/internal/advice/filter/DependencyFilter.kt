package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.HasDependency

internal interface DependencyFilter {
  val predicate: (HasDependency) -> Boolean
}
