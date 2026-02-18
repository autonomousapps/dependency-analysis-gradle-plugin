// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.model.GradleVariantIdentification
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class DeclarationTest {

  // Regression test for a silly mistake with the `compareTo()` implementation.
  @Test fun `declarations are sortable`() {
    // Given
    val declaration1 = Declaration(
      identifier = ":annos",
      version = null,
      configurationName = "api",
      gradleVariantIdentification = GradleVariantIdentification.EMPTY,
    )
    val declaration2 = Declaration(
      identifier = ":property",
      version = null,
      configurationName = "api",
      gradleVariantIdentification = GradleVariantIdentification.EMPTY,
    )
    val declarations = listOf(declaration1, declaration2)

    // List has expected elements, in order
    assertThat(declarations).containsExactly(declaration1, declaration2).inOrder()

    // Expect
    assertThat(declarations.toSortedSet()).containsExactly(declaration1, declaration2).inOrder()
  }
}
