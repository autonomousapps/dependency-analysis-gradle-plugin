package com.autonomousapps.model

import com.autonomousapps.internal.utils.lowercase

enum class SourceSetKind(
  val taskNameSuffix: String,
  private val formatString: String
) {
  // TODO these format strings are Android-specific. This enum might be useful for JVM, too
  MAIN("Main", "%sCompileClasspath"),
  TEST("Test", "%sUnitTestCompileClasspath")
  ;

  fun compileClasspathConfigurationName(variantName: String) = String.format(formatString, variantName)

  val variantName: String = name.lowercase()
}
