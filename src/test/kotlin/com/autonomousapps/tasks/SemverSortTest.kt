package com.autonomousapps.tasks

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SemverSortTest {

  @Test fun `can sort versions correction`() {
    // default (bad) sort
    assertThat(listOf("1.10", "1.2").sorted()).containsExactly("1.10", "1.2").inOrder()

    // improved (good) sort
    assertThat(listOf("1.10", "1.2").sortedVersions()).containsExactly("1.2", "1.10").inOrder()
  }
}
