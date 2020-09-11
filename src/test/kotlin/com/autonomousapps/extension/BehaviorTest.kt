package com.autonomousapps.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BehaviorTest {

  @Test fun `fail is more important than everything`() {
    val fail = Fail()
    val ignore = Ignore
    val warn = Warn()

    // fail vs ignore
    assertThat(listOf(fail, ignore).max()).isEqualTo(fail)
    assertThat(listOf(ignore, fail).max()).isEqualTo(fail)

    // fail vs warn
    assertThat(listOf(fail, warn).max()).isEqualTo(fail)
    assertThat(listOf(warn, fail).max()).isEqualTo(fail)

    // fail vs fail
    assertThat(listOf(fail, Fail()).max()).isInstanceOf(Fail::class.java)
    assertThat(listOf(Fail(), fail).max()).isInstanceOf(Fail::class.java)
  }

  @Test fun `ignore is more important than warn`() {
    val ignore = Ignore
    val warn = Warn()

    // ignore vs ignore
    assertThat(listOf(ignore, warn).max()).isEqualTo(ignore)
    assertThat(listOf(warn, ignore).max()).isEqualTo(ignore)
  }
}