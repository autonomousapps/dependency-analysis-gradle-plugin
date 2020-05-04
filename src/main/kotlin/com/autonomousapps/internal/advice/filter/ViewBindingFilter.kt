package com.autonomousapps.internal.advice.filter

import com.autonomousapps.advice.Dependency

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

  override val predicate: (Dependency) -> Boolean = { dependency ->
    !viewBindingDependencies.contains(dependency.identifier)
  }
}
