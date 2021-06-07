package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import com.autonomousapps.test.emptyZipFile
import com.autonomousapps.test.walkFileTree
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertTrue

class JarReaderTest {

  @get:Rule val tempFolder = TemporaryFolder()

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
    assertTrue { actualCore.size == expectedCore.size }
    actualCore.forEachIndexed { i, it ->
      assertTrue { it.theClass == expectedCore[i] }
    }

    val expectedDb = emptyList<String>()
    assertTrue { actualDb.size == expectedDb.size }
    actualDb.forEachIndexed { i, it ->
      assertTrue { it.theClass == expectedDb[i] }
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

    assertTrue("Actual size is ${actual.size}, expected was ${expected.size}") {
      actual.size == expected.size
    }
    actual.forEachIndexed { i, it ->
      val e = expected[i]
      assertTrue("Expected $it, was $e (actual = $it") { it.theClass == e }
    }
  }

  private fun emptyZipFile() = tempFolder.emptyZipFile()
}
