// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class CollectionsTest {

  @Test fun `keeps multi-release classes targeting the current or older Java versions`() {
    val current = 21

    assertThat(isUnsupportedMultiReleaseClass("META-INF/versions/17/com/example/Foo.class", current)).isFalse()
    assertThat(isUnsupportedMultiReleaseClass("META-INF/versions/21/com/example/Foo.class", current)).isFalse()
  }

  @Test fun `skips multi-release classes targeting a newer Java version`() {
    val current = 21

    assertThat(isUnsupportedMultiReleaseClass("META-INF/versions/25/com/example/Foo.class", current)).isTrue()
    assertThat(isUnsupportedMultiReleaseClass("META-INF/versions/99/Foo.class", current)).isTrue()
  }

  @Test fun `ordinary class entries are never treated as unsupported multi-release classes`() {
    val current = 21

    assertThat(isUnsupportedMultiReleaseClass("com/example/Foo.class", current)).isFalse()
    assertThat(isUnsupportedMultiReleaseClass("META-INF/MANIFEST.MF", current)).isFalse()
    // A package literally named "versions" must not be misinterpreted as a multi-release directory.
    assertThat(isUnsupportedMultiReleaseClass("com/example/versions/Foo.class", current)).isFalse()
  }
}
