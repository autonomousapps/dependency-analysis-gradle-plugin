package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClassSetReaderTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private val shelter = SeattleShelter()

  @Test fun `class files analysis is correct`() {
    // When
    val actualApp = ClassSetReader(
      variantFiles = emptySet(),
      classes = shelter.app.classesDir().walkTopDown().filter { it.isFile }.toSet(),
      layouts = emptySet(),
      testFiles = emptySet()
    ).analyze()

    // Then
    val expectedApp = shelter.app.classReferences()
    assertThat(actualApp.size).isEqualTo(expectedApp.size)
    actualApp.forEachIndexed { i, it ->
      assertThat(it.theClass).isEqualTo(expectedApp[i])
    }
  }
}
