package com.autonomousapps.internal

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AdvisorTest {

    private val project1 = Dependency(":project1")
    private val project2 = Dependency(":project2")
    private val project3 = Dependency(":project3")

    private val library1 = Dependency("some:lib1", "1")
    private val library2 = Dependency("some:lib2", "1")
    private val library3 = Dependency("some:lib3", "1")

    private val library4 = Dependency("some:lib4", "1", "api")
    private val library5 = Dependency("some:lib5", "1", "api")
    private val library6 = Dependency("some:lib6", "1", "implementation")

    @Test fun testChangeAdvice() {
        // Given
        val abiDeps = listOf(library3, library4, library6)
        val allDeclaredDeps = listOf(library4, library5, library6)
        // a precondition to ensure I don't set up the test incorrectly
        assertTrue("No declared deps will have a null configuration") {
            allDeclaredDeps.none { it.configurationName == null }
        }

        // When
        val advisor = Advisor(
            unusedDirectComponents = emptyList(),
            usedTransitiveComponents = emptyList(),
            abiDeps = abiDeps,
            allDeclaredDeps = allDeclaredDeps
        )
        val changeAdvice = advisor.getChangeAdvice()

        // Then
        assertNotNull(changeAdvice)

        val expected = sortedSetOf(
            Advice.change(library5, "implementation"),
            Advice.change(library6, "api")
        )
        val actual = advisor.getAdvices()
        assertEquals(expected, actual, "Expected $expected\nActual   $actual")
    }

    @Test fun testAddAdvice() {
        // Given
        val usedTransitiveComponents = listOf(
            TransitiveComponent(project1, emptySet()),
            TransitiveComponent(project2, emptySet())
        )
        val abiDeps = listOf(project1, project3)

        // When
        val advisor = Advisor(
            unusedDirectComponents = emptyList(),
            usedTransitiveComponents = usedTransitiveComponents,
            abiDeps = abiDeps,
            allDeclaredDeps = emptyList()
        )
        val addAdvice = advisor.getAddAdvice()

        // Then
        assertNotNull(addAdvice, "Add advice should not be null")

        val expected = sortedSetOf(
            Advice.add(project1, "api"),
            Advice.add(project2, "implementation"),
            Advice.add(project3, "api")
        )
        val actual = advisor.getAdvices()
        assertEquals(expected, actual, "Expected $expected\nActual   $actual")
    }

    @Test fun testRemoveAdvice() {
        // Given
        val unusedDirectComponents = listOf(
            UnusedDirectComponent(project1, mutableSetOf(library1)),
            UnusedDirectComponent(project2, mutableSetOf(library2)),
            UnusedDirectComponent(project3, mutableSetOf())
        )
        val advisor = Advisor(
            unusedDirectComponents = unusedDirectComponents,
            usedTransitiveComponents = emptyList(),
            abiDeps = emptyList(),
            allDeclaredDeps = emptyList()
        )

        // When
        val removeAdvice = advisor.getRemoveAdvice()

        // Then
        assertNotNull(removeAdvice, "Remove advice should not be null")

        val expected = sortedSetOf(
            Advice.remove(project1),
            Advice.remove(project2),
            Advice.remove(project3)
        )
        val actual = advisor.getAdvices()
        assertEquals(expected, actual, "Expected $expected\nActual   $actual")
    }
}
