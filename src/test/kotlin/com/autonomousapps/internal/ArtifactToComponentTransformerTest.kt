package com.autonomousapps.internal

import com.autonomousapps.fixtures.SeattleShelterDbModule
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

/**
 * This test treats the values of running the plugin against the
 * [Seattle-Shelter][https://gitlab.com/autonomousapps/seattle-shelter-android] project to be correct, and so a golden
 * value.
 */
class ArtifactToComponentTransformerTest {

    private val fixture = SeattleShelterDbModule()

    @Test fun `can transform artifacts to components`() {
        // Given
        val transformer = ArtifactToComponentTransformer(
            fixture.mockConfiguration,
            fixture.givenArtifacts,
            mock()
        )

        // When
        val actual = transformer.components()

        // Then
        assert(actual == fixture.expectedComponents)
    }
}
