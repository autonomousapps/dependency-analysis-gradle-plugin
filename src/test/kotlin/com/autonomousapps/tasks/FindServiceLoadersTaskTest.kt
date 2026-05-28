// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class FindServiceLoadersTaskTest {

  @Test fun `retains legacy compile classpath accessors`() {
    val methodNames = FindServiceLoadersTask::class.java.methods.mapTo(mutableSetOf()) { it.name }

    assertThat(methodNames).contains("setCompileClasspath")
    assertThat(methodNames).contains("getCompileClasspath")
  }
}
