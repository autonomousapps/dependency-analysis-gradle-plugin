// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Scribe
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class AndroidBlockTest {

  @Test fun `a default android block is empty`() {
    // Given
    val android = AndroidBlock()

    // Expect
    assertThat(android.render(Scribe())).isEqualTo("android {\n}\n")
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(DslKind::class)
  fun `an android block can have just a namespace created via constructor`(dslKind: DslKind) {
    // Given
    val android = AndroidBlock(namespace = "dependency.analysis")

    // Expect
    val actual = android.render(Scribe(dslKind))
    when (dslKind) {
      DslKind.GROOVY -> assertThat(actual).isEqualTo("android {\n  namespace 'dependency.analysis'\n}\n")
      DslKind.KOTLIN -> assertThat(actual).isEqualTo("android {\n  namespace = \"dependency.analysis\"\n}\n")
    }
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(DslKind::class)
  fun `an android block can have just a namespace created via factory`(dslKind: DslKind) {
    // Given
    val android = AndroidBlock.ofNamespace("dependency.analysis")

    // Expect
    val actual = android.render(Scribe(dslKind))
    when (dslKind) {
      DslKind.GROOVY -> assertThat(actual).isEqualTo("android {\n  namespace 'dependency.analysis'\n}\n")
      DslKind.KOTLIN -> assertThat(actual).isEqualTo("android {\n  namespace = \"dependency.analysis\"\n}\n")
    }
  }
}
