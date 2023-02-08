package com.autonomousapps.model

import com.autonomousapps.model.AndroidResSource.AttrRef.Companion.from
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AttrRefTest {

  @Test
  fun `question mark character as inlined text is not parsed as an AttrRef`() {
    assertNull(from("android:text" to "?"))
  }

}
