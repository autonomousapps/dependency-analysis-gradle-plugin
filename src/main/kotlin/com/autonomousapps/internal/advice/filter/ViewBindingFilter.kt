package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.HasDependency

/**
 * Don't suggest anything to do with the following viewbinding dependencies:
 * * androidx.databinding:viewbinding
 */
internal class ViewBindingFilter : DependencyFilter {

  companion object {
    val viewBindingDependencies = listOf(
      "androidx.databinding:viewbinding"
    )
  }

  override val predicate: (HasDependency) -> Boolean = { dependency ->
    !viewBindingDependencies.contains(dependency.dependency.identifier)
  }
}
