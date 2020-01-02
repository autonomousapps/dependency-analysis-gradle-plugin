package com.autonomousapps

import com.autonomousapps.utils.fileFromResource
import org.gradle.api.logging.Logging
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertTrue

class InlineMemberFinderTest {

    private val logger = Logging.getLogger(InlineMemberFinderTest::class.java)

    @Test fun `stdlib-jdk7 has two inline members`() {
        // Given
        val stdlib = ZipFile(fileFromResource("kotlin-stdlib-jdk7-1.3.60.jar"))

        // When
        val actual = InlineMemberFinder(logger, stdlib).find()

        // Then
        val expected = listOf("kotlin.jdk7.*", "kotlin.jdk7.use")
        assertTrue("Was      $actual\nexpected $expected\n") {
            actual == expected
        }
    }

    @Test fun `annotations has no inline members`() {
        // Given
        val annotations = ZipFile(fileFromResource("annotations-13.0.jar"))

        // When
        val actual = InlineMemberFinder(logger, annotations).find()

        // Then
        val expected = emptyList<String>()
        assertTrue("Was      $actual\nexpected $expected\n") {
            actual == expected
        }
    }

    @Test fun `java file has no inline members`() {
        // Given
        val annotations = ZipFile(fileFromResource("guava-28.0-jre.jar"))

        // When
        val actual = InlineMemberFinder(logger, annotations).find()

        // Then
        val expected = emptyList<String>()
        assertTrue("Was      $actual\nexpected $expected\n") {
            actual == expected
        }
    }
}