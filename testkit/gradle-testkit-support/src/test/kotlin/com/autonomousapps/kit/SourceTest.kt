// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SourceTest {
  @ParameterizedTest(name = "{0} -> {1}, {2} -> {3}")
  @CsvSource(
    value = [
      "'', NIL, Foo, NIL",
      "com.foo, NIL, '', NIL",
      "com.foo, com/foo, class Foo, Foo",
      "a.b, a/b, class B, B",
      "aa.b, aa/b, class B, B",
      "a.bb, a/bb, class B, B",
      "a, a, class A, A",
    ],
    nullValues = ["NIL"]
  )
  fun `can parse dir and filename from source`(
    pkg: String?,
    dirPath: String?,
    className: String?,
    fileName: String?,
  ) {
    val builder = Source.kotlin(
      """
      package $pkg

      $className
      """.trimIndent()
    )

    assertThat(builder.dirPath).isEqualTo(dirPath)
    assertThat(builder.fileName).isEqualTo(fileName)
  }
}
