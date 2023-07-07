package com.autonomousapps.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BehaviorTest {

  @Test fun `fail is more important than everything`() {
    val fail = Fail()
    val ignore = Ignore()
    val warn = Warn()

    // fail vs ignore
    assertThat(listOf(fail, ignore).maxOrNull()).isEqualTo(fail)
    assertThat(listOf(ignore, fail).maxOrNull()).isEqualTo(fail)

    // fail vs warn
    assertThat(listOf(fail, warn).maxOrNull()).isEqualTo(fail)
    assertThat(listOf(warn, fail).maxOrNull()).isEqualTo(fail)

    // fail vs fail
    assertThat(listOf(fail, Fail()).maxOrNull()).isInstanceOf(Fail::class.java)
    assertThat(listOf(Fail(), fail).maxOrNull()).isInstanceOf(Fail::class.java)
  }

  @Test fun `ignore is more important than warn`() {
    val ignore = Ignore()
    val warn = Warn()

    // ignore vs ignore
    assertThat(listOf(ignore, warn).maxOrNull()).isEqualTo(ignore)
    assertThat(listOf(warn, ignore).maxOrNull()).isEqualTo(ignore)
  }
}
