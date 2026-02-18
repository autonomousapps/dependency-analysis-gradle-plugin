// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import com.autonomousapps.model.Advice
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.ProjectCoordinates
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class KmpCommonDependenciesTest {

  private companion object {
    @JvmStatic fun testCases(): Stream<Arguments> {
      return Stream.of(
        // Movements are ignored
        Arguments.of("commonMainImplementation", "jvmMainImplementation", null),
        Arguments.of("commonTestImplementation", "jvmTestImplementation", null),
        // Downgrades are ignored
        Arguments.of("commonMainApi", "jvmMainImplementation", null),
        Arguments.of("commonTestApi", "jvmTestImplementation", null),
        // Movement + upgrade are transformed
        Arguments.of("commonMainImplementation", "jvmMainApi", "commonMainApi"),
        Arguments.of("commonTestImplementation", "jvmTestApi", "commonTestApi"),
        // Upgrades are passed through unchanged
        Arguments.of("commonMainImplementation", "commonMainApi", "commonMainApi"),
        Arguments.of("commonTestImplementation", "commonTestApi", "commonTestApi"),
      )
    }
  }

  private val coordinates = ProjectCoordinates(":magic", GradleVariantIdentification.EMPTY)

  @ParameterizedTest(name = "from={0}, to={1} => to={2}")
  @MethodSource("testCases")
  fun test(fromConfiguration: String, originalToConfiguration: String, newToConfiguration: String?) {
    // Given
    val originalAdvice = Advice.ofChange(coordinates, fromConfiguration, originalToConfiguration)

    // When
    val newAdvice = KmpCommonDependencies.ensureUnbroken(originalAdvice)

    // Then
    val expected = if (newToConfiguration == null) {
      null
    } else {
      originalAdvice.copy(toConfiguration = newToConfiguration)
    }
    assertThat(newAdvice).isEqualTo(expected)
  }
}
