// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.model.AndroidResSource.AttrRef
import com.autonomousapps.model.AndroidResSource.AttrRef.Companion.from
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AttrRefTest {

  @Test
  fun `question mark character as inlined text is not parsed as an AttrRef`() {
    assertNull(from("android:text" to "?"))
  }

  @Test
  fun `escaped question mark character followed by text is not parsed as an AttrRef`() {
    assertNull(from("android:text" to "\\?foo"))
  }

  @Test
  fun `text attribute reference is parsed as an AttrRef`() {
    assertEquals(
      AttrRef(type = "attr", id = "foo"),
      from("android:text" to "?foo"),
    )
  }

}
