// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class RootProjectTest {

  @Test fun `plusAssign works on vars`() {
    val builder = RootProject.Builder()
    builder.gradleProperties += "foo=bar"

    assertThat(builder.gradleProperties.toString()).isEqualTo(
      """
        # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
        org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:MaxMetaspaceSize=1024m
        foo=bar
      """.trimIndent()
    )
  }
}
