// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class FlagsTest {

  // Indirectly tests the functionality of the PROJECT_INCLUDES flag.
  @Test fun `validate I understand regex enough for this feature to work`() {
    // match any string except those that start with ':property'
    val regex = Regex("^((?!:property)).*\$")
    val projects = listOf(":annos", ":property", ":proj", ":foo:property")

    val matching = projects.filter {
      regex.matches(it)
    }

    assertThat(matching).containsExactly(":annos", ":proj", ":foo:property")
  }
}
