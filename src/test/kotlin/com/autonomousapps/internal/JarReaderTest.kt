package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import com.autonomousapps.test.emptyZipFile
import com.autonomousapps.test.walkFileTree
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

class JarReaderTest {

  @TempDir lateinit var tempFolder: Path

  private val shelter = SeattleShelter()

  @Test fun `jar file analysis is correct`() {
    // When
    val actualCore = JarReader(
      variantFiles = emptySet(),
      jarFile = shelter.core.jarFile(),
      layouts = emptySet(),
      testFiles = emptySet()
    ).analyze()

    val actualDb = JarReader(
      variantFiles = emptySet(),
      jarFile = shelter.db.jarFile(),
      layouts = emptySet(),
      testFiles = emptySet()
    ).analyze()

    // Then
    val expectedCore = shelter.core.classReferencesInJar()
    assert(actualCore.size == expectedCore.size)
    actualCore.forEachIndexed { i, it ->
      assert(it.theClass == expectedCore[i])
    }

    // TODO one of the elements is null
    val expectedDb = shelter.db.classReferencesInJar()
    assert(actualDb.size == expectedDb.size)
    actualDb.forEachIndexed { i, it ->
      assert(it.theClass == expectedDb[i])
    }
  }

  @Test fun `layout files analysis is correct`() {
    // When
    val actualCore = JarReader(
      variantFiles = emptySet(),
      jarFile = emptyZipFile(),
      layouts = walkFileTree(shelter.core.layoutsPath()),
      testFiles = emptySet()
    ).analyze()

    val actualDb = JarReader(
      variantFiles = emptySet(),
      jarFile = emptyZipFile(),
      layouts = emptySet(),
      testFiles = emptySet()
    ).analyze()

    // Then
    val expectedCore = shelter.core.classReferencesInLayouts()
    assertThat(actualCore.size).isEqualTo(expectedCore.size)
    actualCore.forEachIndexed { i, it ->
      assertThat(it.theClass).isEqualTo(expectedCore[i])
    }

    val expectedDb = emptyList<String>()
    assertThat(actualDb.size).isEqualTo(expectedDb.size)
    actualDb.forEachIndexed { i, it ->
      assertThat(it.theClass).isEqualTo(expectedDb[i])
    }
  }

  @Test fun `lib class usage analysis is correct`() {
    val layoutFiles = walkFileTree(shelter.core.layoutsPath())

    // When
    val actual = JarReader(
      variantFiles = emptySet(),
      jarFile = shelter.core.jarFile(),
      layouts = layoutFiles,
      testFiles = emptySet()
    ).analyze()

    // Then
    // I need a list because I want random access in the assert below
    val expected = with(shelter.core) {
      classReferencesInJar() + classReferencesInLayouts()
    }.toSortedSet().toList()

    // "Actual size is ${actual.size}, expected was ${expected.size}"
    assertThat(actual.size).isEqualTo(expected.size)

    actual.forEachIndexed { i, it ->
      val e = expected[i]
      // "Expected $it, was $e (actual = $it"
      assertThat(it.theClass).isEqualTo(e)
    }
  }

  private fun emptyZipFile(): File = tempFolder.emptyZipFile().toFile()
}
