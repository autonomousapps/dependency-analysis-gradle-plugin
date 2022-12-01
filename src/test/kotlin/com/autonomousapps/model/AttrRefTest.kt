package com.autonomousapps.model

import com.autonomousapps.model.AndroidResSource.AttrRef
import com.autonomousapps.model.AndroidResSource.AttrRef.Companion.from
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AttrRefTest {

  @Test fun `ids are ignored`() {
    assertNull(from("android:id" to "@+id/title"))
    assertNull(from("android:id" to "@id/title"))
  }

  @Test fun `tools attributes are ignored`() {
    assertNull(from("tools:context" to ".MainActivity"))
  }

  @Test fun `data binding expressions are ignored`() {
    // Shouldn't this be
    assertNull(from("binding:text" to "@{`Hello`}"))
  }

  @Test fun `attribute regex`() {
    assertEquals(
      AttrRef(type = "attr", id = "themeColor"),
      from("android:tint" to "?themeColor")
    )
    assertEquals(
      AttrRef(type = "attr", id = "themeColor"),
      from("android:tint" to "?attr/themeColor")
    )
  }

  @Test fun `type regex`() {
    assertEquals(
      AttrRef(type = "drawable", id = "some_drawable"),
      from("android:drawable" to "@drawable/some_drawable")
    )
    assertEquals(
      AttrRef(type = "drawable", id = "some_drawable"),
      from("android:drawable" to "@android:drawable/some_drawable")
    )
  }

  @Test fun `question mark character as inlined text`() {
    assertNull(from("android:text" to "?"))
  }

}
