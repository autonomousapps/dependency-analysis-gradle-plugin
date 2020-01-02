package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertTrue

class ClassSetReaderTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val shelter = SeattleShelter()

    @Test fun `jar file analysis is correct`() {
        // When
        val actualCore = ClassSetReader(
            classes = shelter.app.classesDir().walkTopDown().filter { it.isFile }.toSet(),
            layouts = emptySet(),
            kaptJavaSource = emptySet()
        ).analyze()

        // Then
        val expectedCore = shelter.app.classReferences()
        assertTrue { actualCore.size == expectedCore.size }
        actualCore.forEachIndexed { i, it ->
            assertTrue { it == expectedCore[i] }
        }
    }
}
