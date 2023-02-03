package com.autonomousapps.internal.configuration

import com.autonomousapps.model.declaration.Configurations
import com.autonomousapps.model.declaration.SourceSetKind
import com.autonomousapps.model.declaration.Variant
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ConfigurationsTest {

  private val supportedSourceSets = setOf(
    "main", "release", "debug", "flavorRelease", "flavorDebug",
    "test", "testDebug", "testRelease", "testFlavorRelease", "testFlavorDebug",
    "androidTest", "androidTestDebug", "androidTestRelease", "androidTestFlavorRelease",
  )

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "debugApi, debug",
      "releaseImplementation, release",
      "kaptDebug, debug",
      "flavorReleaseAnnotationProcessor, flavorRelease",
      "implementation, main",
      "api, main",
      "annotationProcessor, main",
      "kapt, main",
    ]
  )
  fun `can get variant from main configuration name`(configuration: String, variant: String) {
    assertThat(Configurations.variantFrom(configuration, supportedSourceSets, false))
      .isEqualTo(Variant(variant, SourceSetKind.MAIN))
  }

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "testDebugApi, debug",
      "testReleaseImplementation, release",
      "kaptTestDebug, debug",
      "testFlavorReleaseAnnotationProcessor, flavorRelease",
      "testImplementation, main",
      "testApi, main",
      "testAnnotationProcessor, main",
      "kaptTest, main",
    ]
  )
  fun `can get variant from test configuration name`(configuration: String, variant: String) {
    assertThat(Configurations.variantFrom(configuration, supportedSourceSets, false))
      .isEqualTo(Variant(variant, SourceSetKind.TEST))
  }

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "androidTestDebugApi, debug",
      "androidTestReleaseImplementation, release",
      "kaptAndroidTestDebug, debug",
      "androidTestFlavorReleaseAnnotationProcessor, flavorRelease",
      "androidTestImplementation, main",
      "androidTestApi, main",
      "androidTestAnnotationProcessor, main",
      "kaptAndroidTest, main",
    ]
  )
  fun `can get variant from androidTest configuration name`(configuration: String, variant: String) {
    assertThat(Configurations.variantFrom(configuration, supportedSourceSets, false))
      .isEqualTo(Variant(variant, SourceSetKind.ANDROID_TEST))
  }

  @Test fun `variant equality works`() {
    assertThat(Variant("main", SourceSetKind.MAIN)).isEqualTo(Variant.MAIN)
    assertThat(Variant("debug", SourceSetKind.MAIN)).isNotEqualTo(Variant.MAIN)
  }
}
