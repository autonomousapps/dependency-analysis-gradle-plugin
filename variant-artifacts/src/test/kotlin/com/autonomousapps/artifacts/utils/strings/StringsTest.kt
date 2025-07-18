package com.autonomousapps.artifacts.utils.strings

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class StringsTest {

  @ParameterizedTest(name = "{0} => {1}")
  @CsvSource(
    value = [
      "foo_bar, fooBar",
      "foobar, foobar",
      "FOO_BAR, fooBar",
      "FOO-BAR, fooBar",
      "foo__bar, fooBar",
      "foo_-bar, fooBar",
      "foo_9-bar, foo9Bar",
      "foo+++bar, fooBar",
      "foo+++Bar, fooBar",
      "foobar+, foobar",
      "+foobar, foobar",
      "+foo_bar, fooBar",
    ]
  )
  fun `camelCase function works`(given: String, expected: String) {
    assertThat(given.camelCase()).isEqualTo(expected)
    assertThat("foo_bar".camelCase()).isEqualTo("fooBar")
    assertThat("foobar".camelCase()).isEqualTo("foobar")
    assertThat("FOO_BAR".camelCase()).isEqualTo("fooBar")
    assertThat("FOO-BAR".camelCase()).isEqualTo("fooBar")
  }

  @Test fun `camelCase of an empty string is an empty string`() {
    assertThat("".camelCase()).isEmpty()
  }
}
