// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.source

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SourceKindTest {

  @Test fun `android, then jvm, then kmp`() {
    // Given three source kinds
    val androidSourceKind = AndroidSourceKind.main("debug")
    val jvmSourceKind = JvmSourceKind.of("main")
    val kmpSourceKind = KmpSourceKind.of("jvmMain")

    // When we sort them
    val sortedSet = sortedSetOf(jvmSourceKind, kmpSourceKind, androidSourceKind)

    // Then they're in the expected order (by type)
    assertThat(sortedSet).containsExactly(androidSourceKind, jvmSourceKind, kmpSourceKind).inOrder()
  }
}
