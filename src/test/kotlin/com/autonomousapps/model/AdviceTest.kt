/*
 * HIYA CONFIDENTIAL
 * __________________
 *
 * (c) 2020 Hiya, Inc.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Hiya, Inc. The intellectual and technical
 * concepts contained herein are proprietary to Hiya, Inc.
 * may be covered by U.S. and foreign patents, and are
 * protected by trade secret or copyright law.  Dissemination
 * of or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from Hiya, Inc.
 */

package com.autonomousapps.model

import com.autonomousapps.internal.utils.flatMapToOrderedSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdviceTest {

  /**
   * This test exists due to the discovery of a subtle bug in [Advice.compareTo] and how that
   * interacts with [java.util.TreeSet].
   */
  @Test fun `an ordered set of advice contains no duplicates`() {
    // Given
    val androidxLifecycle = ModuleCoordinates("androidx.lifecycle:lifecycle-common8", "n/a")
    val adviceSet1 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val adviceSet2 = setOf(Advice.ofRemove(androidxLifecycle, "foo"))
    val list = listOf(adviceSet1, adviceSet2)

    // When
    val orderedSet = list.flatMapToOrderedSet { it }

    // Then
    assertThat(orderedSet.size).isEqualTo(1)
  }
}
