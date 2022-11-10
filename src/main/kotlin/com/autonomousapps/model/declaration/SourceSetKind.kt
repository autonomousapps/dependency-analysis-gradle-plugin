package com.autonomousapps.model.declaration

import com.autonomousapps.internal.utils.lowercase
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class SourceSetKind(
  val taskNameSuffix: String,
  private val compileClasspathFormatString: String,
  private val runtimeClasspathFormatString: String
) {
  // TODO V2: these format strings are Android-specific. This enum might be useful for JVM, too
  MAIN("Main", "%sCompileClasspath", "%sRuntimeClasspath"),
  TEST("Test", "%sUnitTestCompileClasspath", "%sUnitTestRuntimeClasspath"),
  ANDROID_TEST("androidTest", "%sAndroidTestCompileClasspath", "%sAndroidTestRuntimeClasspath")
  ;

  fun compileClasspathConfigurationName(variantName: String) = String.format(compileClasspathFormatString, variantName)
  fun runtimeClasspathConfigurationName(variantName: String) = String.format(runtimeClasspathFormatString, variantName)

  /**
   * Returns the primary/standard/"base" [variant][Variant] for this source set. E.g., for [MAIN], the primary variant
   * is ("main", MAIN) and corresponds to the source set `src/main`. Similarly for [TEST], the primary variant is
   * ("test", TEST) and corresponds to the source set `src/test`. In contrast, in the Android ecosystem, there are
   * variants like ("debug", MAIN), ("release", TEST), etc., and which correspond to the source sets `src/main` and
   * `src/test` (+ `src/release`, because "unit tests" in the Android world are the combination of TEST source and the
   * variant-specific MAIN source).
   */
  fun asBaseVariant() = Variant(name.lowercase(), this)
}
