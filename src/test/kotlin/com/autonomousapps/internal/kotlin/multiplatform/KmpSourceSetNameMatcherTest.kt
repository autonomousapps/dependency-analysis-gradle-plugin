// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.kotlin.multiplatform

import com.autonomousapps.model.internal.intermediates.ClassReference
import com.autonomousapps.model.source.KmpSourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class KmpSourceSetNameMatcherTest {

  @Test fun `a class ref to non-common is unchanged`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(ClassReference("foo", "barMain"))

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback)
  }

  @Test fun `a class ref to commonMain is commonMain`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(ClassReference("foo", "commonMain"))

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback.copy(name = "commonMain"))
  }

  @Test fun `a class ref to commonMain and commonTest is commonMain`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(
      ClassReference("foo", "commonTest"),
      ClassReference("bar", "commonMain")
    )

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback.copy(name = "commonMain"))
  }

  @Test fun `a class ref to commonTest is commonTest`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(ClassReference("foo", "commonTest"))

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback.copy(name = "commonTest"))
  }

  @Test fun `a class ref to commonJvmMain is commonJvmMain`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(ClassReference("foo", "commonJvmMain"))

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback.copy(name = "commonJvmMain"))
  }

  @Test fun `a class ref to commonJvmTest is commonJvmTest`() {
    // Given
    val fallback = KmpSourceKind.of("jvmMain")
    val classes = setOf(ClassReference("foo", "commonJvmTest"))

    // When
    val rootMost = KmpSourceSetNameMatcher.getRootMostSourceKind(classes, fallback)

    // Then
    assertThat(rootMost).isEqualTo(fallback.copy(name = "commonJvmTest"))
  }
}
