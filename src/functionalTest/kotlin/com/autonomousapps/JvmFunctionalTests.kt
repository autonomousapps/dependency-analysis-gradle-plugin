package com.autonomousapps

import com.autonomousapps.fixtures.*
import com.autonomousapps.utils.build
import com.autonomousapps.utils.forEachPrinting
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class JvmFunctionalTests : AbstractFunctionalTests() {

    @Test fun `finds constants in java projects`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(CONSUMER_CONSTANT_JAVA, PRODUCER_CONSTANT_JAVA)
            )

            // When
            build(gradleVersion, javaLibraryProject, "buildHealth")

            // Then
            val actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_JAVA)
            assertTrue("Expected nothing, got $actualUnusedDependencies") {
                emptyList<String>() == actualUnusedDependencies
            }

            // Cleanup
            cleanup(javaLibraryProject)
        }
    }

    // This test currently fails, since Kotlin bytecode doesn't have the same properties as Java bytecode :'(
    @Ignore
    @Test fun `finds constants in kotlin projects`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(CONSUMER_CONSTANT_KOTLIN, PRODUCER_CONSTANT_KOTLIN)
            )

            // When
            build(gradleVersion, javaLibraryProject, "buildHealth")

            // Then
            val actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(CONSUMER_CONSTANT_KOTLIN)
            assertTrue("Expected nothing, got $actualUnusedDependencies") {
                emptyList<String>() == actualUnusedDependencies
            }

            // Cleanup
            cleanup(javaLibraryProject)
        }
    }

    @Test fun `correctly analyzes JVM projects for inline usage`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(INLINE_PARENT, INLINE_CHILD)
            )

            // When
            build(gradleVersion, javaLibraryProject, "buildHealth")

            // Then
            val actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(INLINE_PARENT)
            assertTrue("Expected kotlin-stdlib-jdk8, got $actualUnusedDependencies") {
                listOf("org.jetbrains.kotlin:kotlin-stdlib-jdk8") == actualUnusedDependencies
            }

            // Cleanup
            cleanup(javaLibraryProject)
        }
    }

    @Test fun `does not declare superclass used when it's only needed for compilation`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library with a consumer that uses a child, and a child
            // with an ABI that requires super.
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(ABI_SUPER_LIB, ABI_CHILD_LIB, ABI_CONSUMER_LIB)
            )

            // When
            build(gradleVersion, javaLibraryProject, "buildHealth")

            // Then
            // The SuperClass of ChildClass is not considered used-by Consumer.
            val actualUsedClasses = javaLibraryProject.allUsedClassesFor(ABI_CONSUMER_LIB)
            val expected = listOf("$DEFAULT_PACKAGE_NAME.kotlin.ChildClass", "$DEFAULT_PACKAGE_NAME.kotlin.ConsumerClass", "kotlin.Metadata")
            assertTrue("Expected $expected\nActual $actualUsedClasses") {
                expected == actualUsedClasses
            }

            val actualChild = javaLibraryProject.allUsedClassesFor(ABI_CHILD_LIB)
            val expectedChild = listOf("$DEFAULT_PACKAGE_NAME.kotlin.ChildClass", "$DEFAULT_PACKAGE_NAME.kotlin.SuperClass", "kotlin.Metadata")
            assertTrue("Expected $expectedChild\nActual $actualChild") {
                expectedChild == actualChild
            }

            // Cleanup
            cleanup(javaLibraryProject)
        }
    }
}
