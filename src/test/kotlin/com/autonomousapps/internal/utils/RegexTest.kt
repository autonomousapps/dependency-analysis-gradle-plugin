// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class RegexTest {

  /** See asm.kt, `visitInvokeDynamicInsn(..., descriptor: String?, ...)` */
  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "()Lcom/example/producer/java/SamInterface;, Lcom/example/producer/java/SamInterface;",
      "(Lcom/example/Magic;)Lcom/example/producer/java/SamInterface;, 'Lcom/example/Magic;, Lcom/example/producer/java/SamInterface;'",
    ]
  )
  fun `should parse types from invoke dynamics`(descriptor: String, expectedMatches: String) {
    assertThat(JAVA_FQCN_REGEX_ASM.findAll(descriptor).map { it.value }.toList())
      .containsExactlyElementsIn(expectedMatches.split(", "))
  }
}
