package com.autonomousapps

import com.autonomousapps.fixtures.CHILD
import com.autonomousapps.fixtures.MultiModuleJavaLibraryProject
import com.autonomousapps.fixtures.PARENT
import com.autonomousapps.utils.build
import com.autonomousapps.utils.forEachPrinting
import org.apache.commons.io.FileUtils
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class JvmFunctionalTests : AbstractFunctionalTests() {

    @Test fun `correctly analyzes JVM projects for inline usage`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(PARENT, CHILD)
            )

            // When
            build(gradleVersion, javaLibraryProject, "buildHealth")

            // Then
            val actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(PARENT)
            assertTrue("Expected kotlin-stdlib-jdk8, got $actualUnusedDependencies") {
                listOf("org.jetbrains.kotlin:kotlin-stdlib-jdk8") == actualUnusedDependencies
            }

            // Cleanup
            FileUtils.deleteDirectory(javaLibraryProject.projectDir)
        }
    }

    @Test fun test() {

    }
}