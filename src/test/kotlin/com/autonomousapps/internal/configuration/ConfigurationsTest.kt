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

  @DisplayName("Configurations.isVariant()")
  @ParameterizedTest(name = "isVariant({0}) == {1}")
  @CsvSource(
    value = [
      "debugApi, true",
      "releaseImplementation, true",
      "kaptDebug, true",
      "kapt, false",
      "debugAnnotationProcessor, true",
      "annotationProcessor, false",
      "implementation, false",
      "api, false"
    ]
  )
  fun `is variant`(candidate: String, value: Boolean) {
    assertThat(Configurations.isVariant(candidate)).isEqualTo(value)
  }

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "debugApi, debug",
      "releaseImplementation, release",
      "kaptDebug, debug",
      "releaseFlavorAnnotationProcessor, releaseFlavor",
      "implementation, main",
      "api, main",
      "annotationProcessor, main",
      "kapt, main",
    ]
  )
  fun `can get variant from main configuration name`(configuration: String, variant: String) {
    assertThat(Configurations
      .variantFrom(configuration, setOf(
        "main", "release", "debug", "test", "testDebug", "testRelease",
        "releaseFlavor", "debugFlavor", "testReleaseFlavor", "testDebugFlavor")))
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
    assertThat(Configurations
      .variantFrom(configuration, setOf(
        "main", "release", "debug", "test", "testDebug", "testRelease",
        "flavor", "flavorRelease", "flavorDebug", "testFlavorRelease", "testFlavorDebug")))
      .isEqualTo(Variant(variant, SourceSetKind.TEST))
  }

  @Test fun `variant equality works`() {
    assertThat(Variant("main", SourceSetKind.MAIN)).isEqualTo(Variant.MAIN)
    assertThat(Variant("debug", SourceSetKind.MAIN)).isNotEqualTo(Variant.MAIN)
  }
}
