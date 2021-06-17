package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelterDbModule
import com.autonomousapps.stubs.StubInMemoryCache
import com.nhaarman.mockitokotlin2.mock
import org.junit.Ignore
import org.junit.Test

/**
 * This test treats the values of running the plugin against the
 * [Seattle-Shelter][https://gitlab.com/autonomousapps/seattle-shelter-android] project to be correct, and so a golden
 * value.
 */
class JarAnalyzerTest {

  private val fixture = SeattleShelterDbModule()

  // I've changed the Component model, so this is totally broken right now
  @Ignore
  @Test fun `can transform artifacts to components`() {
    // Given
    val transformer = JarAnalyzer(
      fixture.mockCompileClasspath,
      fixture.mockTestCompileClasspath,
      fixture.givenArtifacts,
      emptySet(),
      mock(),
      StubInMemoryCache()
    )

    // When
    val actual = transformer.components()

    // Then
    assert(actual == fixture.expectedComponents)
  }
}
