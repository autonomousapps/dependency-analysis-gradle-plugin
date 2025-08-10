// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.declaration.internal

import com.autonomousapps.model.internal.declaration.Configurations
import com.autonomousapps.model.source.AndroidSourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ConfigurationsTest {

  private val supportedSourceSets = setOf(
    "main",
    "release", "debug",
    "flavorRelease", "flavorDebug",
    "test",
    "testDebug", "testRelease",
    "testFlavorRelease", "testFlavorDebug",
    "androidTest",
    "androidTestDebug", "androidTestRelease",
    "androidTestFlavorRelease",
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
  fun `can get sourceKind from main configuration name`(configuration: String, variant: String) {
    val actual = Configurations.sourceKindFrom(
      configuration,
      supportedSourceSets,
      isAndroidProject = true,
      hasCustomSourceSets = false,
    )
    val expected = AndroidSourceKind.main(variant)

    assertThat(actual).isEqualTo(expected)
  }

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "testDebugApi, debug",
      "testReleaseImplementation, release",
      "kaptTestDebug, debug",
      "testFlavorReleaseAnnotationProcessor, flavorRelease",
      "testImplementation, test",
      "testApi, test",
      "testAnnotationProcessor, test",
      "kaptTest, test",
    ]
  )
  fun `can get sourceKind from test configuration name`(configuration: String, variant: String) {
    val actual = Configurations.sourceKindFrom(
      configuration,
      supportedSourceSets,
      isAndroidProject = true,
      hasCustomSourceSets = false,
    )
    val expected = AndroidSourceKind.test(variant)

    assertThat(actual).isEqualTo(expected)
  }

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "androidTestDebugApi, debug",
      "androidTestReleaseImplementation, release",
      "kaptAndroidTestDebug, debug",
      "androidTestFlavorReleaseAnnotationProcessor, flavorRelease",
      "androidTestImplementation, androidTest",
      "androidTestApi, androidTest",
      "androidTestAnnotationProcessor, androidTest",
      "kaptAndroidTest, androidTest",
    ]
  )
  fun `can get sourceKind from androidTest configuration name`(configuration: String, variant: String) {
    val actual = Configurations.sourceKindFrom(
      configuration,
      supportedSourceSets,
      isAndroidProject = true,
      hasCustomSourceSets = false,
    )
    val expected = AndroidSourceKind.androidTest(variant)

    assertThat(actual).isEqualTo(expected)
  }
}
