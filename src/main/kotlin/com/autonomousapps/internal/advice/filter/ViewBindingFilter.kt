package com.autonomousapps.internal.advice.filter

/**
 * Don't suggest anything to do with the following viewbinding dependencies:
 * * androidx.databinding:viewbinding
 */
internal class ViewBindingFilter {

  companion object {
    val viewBindingDependencies = listOf(
      "androidx.databinding:viewbinding"
    )
  }
}
