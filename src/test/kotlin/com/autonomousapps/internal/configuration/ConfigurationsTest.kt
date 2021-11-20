package com.autonomousapps.internal.configuration

import com.autonomousapps.model.intermediates.Variant
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
      "annotationProcessorReleaseFlavor, releaseFlavor",
      "implementation, main",
      "api, main",
      "annotationProcessor, main",
      "kapt, main",
    ]
  )
  fun `can get variant from configuration name`(configuration: String, variant: String) {
    assertThat(Configurations.variantFrom(configuration)).isEqualTo(Variant(variant))
  }

  @Test fun `variant equality works`() {
    assertThat(Variant("main")).isEqualTo(Variant.MAIN)
    assertThat(Variant("debug")).isNotEqualTo(Variant.MAIN)
  }
}
