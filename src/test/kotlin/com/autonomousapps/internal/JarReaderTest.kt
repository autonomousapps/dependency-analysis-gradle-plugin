package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import com.autonomousapps.utils.emptyZipFile
import com.autonomousapps.utils.walkFileTree
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
      jarFile = shelter.core.jarFile(),
      layouts = emptySet(),
      kaptJavaSource = emptySet(),
      variantFiles = emptySet()
    ).analyze()

    val actualDb = JarReader(
      jarFile = shelter.db.jarFile(),
      layouts = emptySet(),
      kaptJavaSource = emptySet(),
      variantFiles = emptySet()
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
      jarFile = emptyZipFile(),
      layouts = walkFileTree(shelter.core.layoutsPath()),
      kaptJavaSource = emptySet(),
      variantFiles = emptySet()
    ).analyze()

    val actualDb = JarReader(
      jarFile = emptyZipFile(),
      layouts = emptySet(),
      kaptJavaSource = emptySet(),
      variantFiles = emptySet()
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

  @Test fun `kaptJavaSource analysis is correct`() {
    // When
    val actualCore = JarReader(
      jarFile = emptyZipFile(),
      layouts = emptySet(),
      kaptJavaSource = walkFileTree(shelter.core.kaptStubsPath()) {
        it.toFile().path.endsWith(".java")
      },
      variantFiles = emptySet()
    ).analyze()

    val actualDb = JarReader(
      jarFile = emptyZipFile(),
      layouts = emptySet(),
      kaptJavaSource = walkFileTree(shelter.db.kaptStubsPath()) {
        it.toFile().path.endsWith(".java")
      },
      variantFiles = emptySet()
    ).analyze()

    // Then
    val expectedCore = shelter.core.classReferencesInKaptStubs()
    assertTrue { actualCore.size == expectedCore.size }
    actualCore.forEachIndexed { i, it ->
      assertTrue { it.theClass == expectedCore[i] }
    }

    // TODO Retrofit.Builder?
    val expectedDb = shelter.db.classReferencesInKaptStubs()
    assertTrue { actualDb.size == expectedDb.size }
    actualDb.forEachIndexed { i, it ->
      assertTrue { it.theClass == expectedDb[i] }
    }
  }

  @Test fun `lib class usage analysis is correct`() {
    val layoutFiles = walkFileTree(shelter.core.layoutsPath())
    val kaptStubFiles = walkFileTree(shelter.core.kaptStubsPath()) {
      it.toFile().path.endsWith(".java")
    }

    // When
    val actual = JarReader(
      jarFile = shelter.core.jarFile(),
      layouts = layoutFiles,
      kaptJavaSource = kaptStubFiles,
      variantFiles = emptySet()
    ).analyze()

    // Then
    // I need a list because I want random access in the assert below
    val expected = with(shelter.core) {
      classReferencesInJar() + classReferencesInLayouts() + classReferencesInKaptStubs()
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
