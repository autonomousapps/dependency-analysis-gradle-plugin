// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0

package com.autonomousapps.model

import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AdviceTest {

  /**
   * This test exists due to the discovery of a subtle bug in [Advice.compareTo] and how that interacts with
   * [java.util.TreeSet].
   */
  @Test fun `an ordered set of advice contains no duplicates`() {
    // Given
    val androidxLifecycle =
      ModuleCoordinates("androidx.lifecycle:lifecycle-common8", "n/a", GradleVariantIdentification.EMPTY)
    val adviceSet1 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val adviceSet2 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val list = listOf(adviceSet1, adviceSet2)

    // When
    val orderedSet = list.flatMapToOrderedSet { it }

    // Then
    assertThat(orderedSet.size).isEqualTo(1)
  }
}
