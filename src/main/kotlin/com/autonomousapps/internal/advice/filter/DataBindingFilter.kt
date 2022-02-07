package com.autonomousapps.internal.advice.filter

/**
 * Don't suggest anything to do with the following databinding dependencies:
 * * androidx.databinding:databinding-adapters
 * * androidx.databinding:databinding-runtime
 * * androidx.databinding:databinding-common
 * * androidx.databinding:databinding-compiler
 *
 * For AGP 7+
 * * androidx.databinding:databinding-ktx
 */
internal class DataBindingFilter {

  companion object {
    val databindingDependencies = listOf(
      "androidx.databinding:databinding-adapters",
      "androidx.databinding:databinding-runtime",
      "androidx.databinding:databinding-common",
      "androidx.databinding:databinding-compiler",
      "androidx.databinding:databinding-ktx"
    )
  }
}
