package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelter
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertTrue

class ClassSetReaderTest {

    @get:Rule val tempFolder = TemporaryFolder()

    private val shelter = SeattleShelter()

    @Test fun `class files analysis is correct`() {
        // When
        val actualApp = ClassSetReader(
            classes = shelter.app.classesDir().walkTopDown().filter { it.isFile }.toSet(),
            layouts = emptySet(),
            kaptJavaSource = emptySet()
        ).analyze()

        // Then
        val expectedApp = shelter.app.classReferences()
        assertTrue { actualApp.size == expectedApp.size }
        actualApp.forEachIndexed { i, it ->
            assertTrue { it == expectedApp[i] }
        }
    }
}
