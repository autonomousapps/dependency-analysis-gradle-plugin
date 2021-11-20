package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.HasDependency

/**
 * Don't suggest anything to do with the following databinding dependencies:
 * * androidx.databinding:databinding-adapters
 * * androidx.databinding:databinding-runtime
 * * androidx.databinding:databinding-common
 *
 * For AGP 7+
 * * androidx.databinding:databinding-ktx
 */
internal class DataBindingFilter : DependencyFilter {

  companion object {
    val databindingDependencies = listOf(
      "androidx.databinding:databinding-adapters",
      "androidx.databinding:databinding-runtime",
      "androidx.databinding:databinding-common",
      "androidx.databinding:databinding-ktx"
    )
  }

  override val predicate: (HasDependency) -> Boolean = { dependency ->
    !databindingDependencies.contains(dependency.dependency.identifier)
  }
}
