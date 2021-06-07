package com.autonomousapps

import com.autonomousapps.internal.utils.getLogger
import com.autonomousapps.stubs.StubInMemoryCache
import com.autonomousapps.stubs.StubProperty
import com.autonomousapps.tasks.InlineMemberFinder
import com.autonomousapps.test.fileFromResource
import org.gradle.api.provider.Property
import org.junit.Test
import java.util.zip.ZipFile

class InlineMemberFinderTest {

  private val logger = getLogger<InlineMemberFinderTest>()

  @Test fun `stdlib-jdk7 has two inline members`() {
    // Given
    val cacheProperty: Property<StubInMemoryCache> = StubProperty(StubInMemoryCache())
    val stdlib = ZipFile(fileFromResource("kotlin-stdlib-jdk7-1.3.60.jar"))

    // When
    val actual = InlineMemberFinder(cacheProperty, logger, stdlib).find()

    // Then
    val expected = listOf("kotlin.jdk7.*", "kotlin.jdk7.use")
    assert(actual == expected)
  }

  @Test fun `annotations has no inline members`() {
    // Given
    val cacheProperty: Property<StubInMemoryCache> = StubProperty(StubInMemoryCache())
    val annotations = ZipFile(fileFromResource("annotations-13.0.jar"))

    // When
    val actual = InlineMemberFinder(cacheProperty, logger, annotations).find()

    // Then
    val expected = emptyList<String>()
    assert(actual == expected)
  }

  @Test fun `java file has no inline members`() {
    // Given
    val cacheProperty: Property<StubInMemoryCache> = StubProperty(StubInMemoryCache())
    val annotations = ZipFile(fileFromResource("guava-28.0-jre.jar"))

    // When
    val actual = InlineMemberFinder(cacheProperty, logger, annotations).find()

    // Then
    val expected = emptyList<String>()
    assert(actual == expected)
  }
}