package com.autonomousapps.internal

import com.autonomousapps.emptyZipFile
import com.autonomousapps.walkFileTree
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

class JarReaderTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val shelter = SeattleShelter(javaClass.classLoader)

    @Test fun `jar file analysis is correct`() {
        // When
        val actualCore = JarReader(
            jarFile = shelter.core.jarPath().toFile(),
            layouts = emptySet(),
            kaptJavaSource = emptySet()
        ).analyze()

        val actualDb = JarReader(
            jarFile = shelter.db.jarPath().toFile(),
            layouts = emptySet(),
            kaptJavaSource = emptySet()
        ).analyze()

        // Then
        val expectedCore = shelter.core.classReferencesInJar()
        assertTrue { actualCore.size == expectedCore.size }
        actualCore.forEachIndexed { i, it ->
            assertTrue { it == expectedCore[i] }
        }

        // TODO one of the elements is null
        val expectedDb = shelter.db.classReferencesInJar()
        assertTrue { actualDb.size == expectedDb.size }
        actualDb.forEachIndexed { i, it ->
            assertTrue { it == expectedDb[i] }
        }
    }

    @Test fun `layout files analysis is correct`() {
        // When
        val actualCore = JarReader(
            jarFile = emptyZipFile(),
            layouts = walkFileTree(shelter.core.layoutsPath()),
            kaptJavaSource = emptySet()
        ).analyze()

        val actualDb = JarReader(
            jarFile = emptyZipFile(),
            layouts = walkFileTree(shelter.db.layoutsPath()),
            kaptJavaSource = emptySet()
        ).analyze()

        // Then
        val expectedCore = shelter.core.classReferencesInLayouts()
        assertTrue { actualCore.size == expectedCore.size }
        actualCore.forEachIndexed { i, it ->
            assertTrue { it == expectedCore[i] }
        }

        val expectedDb = emptyList<String>()
        assertTrue { actualDb.size == expectedDb.size }
        actualDb.forEachIndexed { i, it ->
            assertTrue { it == expectedDb[i] }
        }
    }

    @Test fun `kaptJavaSource analysis is correct`() {
        // When
        val actualCore = JarReader(
            jarFile = emptyZipFile(),
            layouts = emptySet(),
            kaptJavaSource = walkFileTree(shelter.core.kaptStubs()) {
                it.toFile().path.endsWith(".java")
            }
        ).analyze()

        val actualDb = JarReader(
            jarFile = emptyZipFile(),
            layouts = emptySet(),
            kaptJavaSource = walkFileTree(shelter.db.kaptStubs()) {
                it.toFile().path.endsWith(".java")
            }
        ).analyze()

        // Then
        val expectedCore = shelter.core.classReferencesInKaptStubs()
        assertTrue { actualCore.size == expectedCore.size }
        actualCore.forEachIndexed { i, it ->
            assertTrue { it == expectedCore[i] }
        }

        // TODO Retrofit.Builder?
        val expectedDb = shelter.db.classReferencesInKaptStubs()
        assertTrue { actualDb.size == expectedDb.size }
        actualDb.forEachIndexed { i, it ->
            assertTrue { it == expectedDb[i] }
        }
    }

    @Test fun `lib class usage analysis is correct`() {
        val layoutFiles = walkFileTree(shelter.core.layoutsPath())
        val kaptStubFiles = walkFileTree(shelter.core.kaptStubs()) {
            it.toFile().path.endsWith(".java")
        }

        // When
        val actual = JarReader(
            jarFile = shelter.core.jarPath().toFile(),
            layouts = layoutFiles,
            kaptJavaSource = kaptStubFiles
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
            assertTrue("Expected $it, was $e (actual = $it") { it == e }
        }
    }

    private fun emptyZipFile() = tempFolder.emptyZipFile()
}

private interface ResourceAware {

    val classLoader: ClassLoader

    fun resource(path: String): Path = Paths.get(classLoader.getResource(path)!!.toURI())
}

/**
 * Testing against an open source Android project,
 * [seattle-shelter][https://gitlab.com/autonomousapps/seattle-shelter-android]. VCS revision
 * [726b501a][https://gitlab.com/autonomousapps/seattle-shelter-android/tree/726b501a1df34eddea9a0879b8cbdc0813c4cebc].
 * Relevant files have been copied directly into test/resources for ease of test development.
 *
 * Treating it as a golden value.
 */
internal class SeattleShelter(override val classLoader: ClassLoader) : ResourceAware {

    private val root = "shelter"

    val core = AndroidLibraryModule(classLoader, "$root/core")
    val db = AndroidLibraryModule(classLoader, "$root/db")

    internal class AndroidLibraryModule(
        override val classLoader: ClassLoader, private val root: String
    ) : ResourceAware {

        fun jarPath(): Path = resource("$root/classes.jar")
        fun layoutsPath(): Path = resource("$root/layouts")
        fun kaptStubs(): Path = resource("$root/kapt-stubs")

        fun classReferencesInJar() =
            resource("$root/classes-jar-expected.txt")
                .toFile()
                .readLines()

        fun classReferencesInLayouts() =
            resource("$root/classes-layouts-expected.txt")
                .toFile()
                .readLines()

        fun classReferencesInKaptStubs() =
            resource("$root/kapt-stubs-expected.txt")
                .toFile()
                .readLines()
    }
}
