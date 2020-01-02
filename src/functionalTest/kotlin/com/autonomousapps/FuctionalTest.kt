package com.autonomousapps

import com.autonomousapps.fixtures.*
import com.autonomousapps.internal.*
import com.autonomousapps.utils.TestMatrix
import com.autonomousapps.utils.build
import com.autonomousapps.utils.forEachPrinting
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    private val testMatrix = TestMatrix()

    @BeforeTest fun cleanWorkspace() {
        // Same as androidProject.projectDir, but androidProject has not been instantiated yet
        FileUtils.deleteDirectory(File(WORKSPACE))
    }

    @Test fun `can execute buildHealth`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project with an app module and a single android-lib module
            val androidProject = AndroidProject(
                agpVersion = agpVersion,
                librarySpecs = listOf(
                    LibrarySpec(
                        name = "lib",
                        type = LibraryType.KOTLIN_ANDROID
                    ),
                    LibrarySpec(
                        name = "java_lib",
                        type = LibraryType.JAVA_JVM
                    ),
                    LibrarySpec(
                        name = "kotlin_lib",
                        type = LibraryType.KOTLIN_JVM
                    )
                )
            )

            // When
            val result = build(
                gradleVersion,
                androidProject,
                "buildHealth", "--rerun-tasks"
            )

            // Then
            // Did expected tasks run?
            // ...in the root project?
            assertTrue { result.output.contains("Task :abiReport") }
            assertTrue { result.output.contains("Task :misusedDependenciesReport") }
            assertTrue { result.output.contains("Task :buildHealth") }

            // ...in the app project?
            assertTrue { result.output.contains("Task :app:misusedDependenciesDebug") }

            // ...in the lib project?
            assertTrue { result.output.contains("Task :lib:misusedDependenciesDebug") }
            assertTrue { result.output.contains("Task :lib:abiAnalysisDebug") }

            // Verify unused dependencies reports
            val actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor("app")
            assertTrue { result.output.contains("Completely unused dependencies") }
            assertTrue("Actual unused app dependencies were $actualUnusedDepsForApp") {
                actualUnusedDepsForApp == listOf(
                    ":java_lib",
                    "androidx.constraintlayout:constraintlayout",
                    "com.google.android.material:material"
                )
            }

            val actualUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor("lib")
            assertTrue { actualUnusedDepsForLib == listOf("androidx.constraintlayout:constraintlayout") }

            // Verify ABI reports
            val actualAbi = androidProject.abiReportFor("lib")
            assertTrue { result.output.contains("These are your API dependencies") }
            assertTrue { actualAbi == listOf("androidx.core:core") }

            // Final result
            assertTrue { result.output.contains("BUILD SUCCESSFUL") }

            // Cleanup
            FileUtils.deleteDirectory(androidProject.projectDir)
        }
    }

    @Test fun `core ktx is a direct dependency`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project with an app module and a single android-lib module
            val libName = "lib"
            val androidProject = AndroidProject(
                agpVersion = agpVersion,
                librarySpecs = listOf(
                    LibrarySpec(
                        name = libName,
                        type = LibraryType.KOTLIN_ANDROID,
                        dependencies = listOf(
                            "implementation" to "androidx.core:core-ktx:1.1.0"
                        ),
                        sources = CORE_KTX_LIB
                    )
                )
            )

            // When
            val result = build(
                gradleVersion,
                androidProject,
                "buildHealth", "--rerun-tasks"
            )

            // Then
            // Did expected tasks run?
            // ...in the lib project?
            assertTrue { result.output.contains("Task :$libName:misusedDependenciesDebug") }
            assertTrue { result.output.contains("Task :$libName:abiAnalysisDebug") }

            // Verify unused dependencies reports
            val actualCompletelyUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor(libName)
            assertTrue("Expected an empty list, got $actualCompletelyUnusedDepsForLib") {
                actualCompletelyUnusedDepsForLib == emptyList<String>()
            }

            val actualUnusedDependencies = androidProject.unusedDependenciesFor(libName)
            assertTrue("Expected an empty list, got $actualUnusedDependencies") {
                actualUnusedDependencies == emptyList<String>()
            }

            // Verify ABI reports
            val actualAbi = androidProject.abiReportFor(libName)
            assertTrue { result.output.contains("These are your API dependencies") }
            assertTrue("Expected an empty list, got $actualAbi") {
                actualAbi == emptyList<String>()
            }

            // Final result
            assertTrue { result.output.contains("BUILD SUCCESSFUL") }

            // Cleanup
            FileUtils.deleteDirectory(androidProject.projectDir)
        }
    }

    @Test fun `correctly analyzes JVM projects for inline usage`() {
        testMatrix.gradleVersions.forEachPrinting { gradleVersion ->
            // Given a multi-module Java library
            val javaLibraryProject = MultiModuleJavaLibraryProject(
                librarySpecs = listOf(PARENT, CHILD)
            )

            // When
            build(
                gradleVersion,
                javaLibraryProject,
                "buildHealth", "--rerun-tasks"
            )

            // Then
            val actualUnusedDependencies = javaLibraryProject.unusedDependenciesFor(PARENT)
            assertTrue("Expected kotlin-stdlib-jdk8, got $actualUnusedDependencies") {
                actualUnusedDependencies == listOf("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            }

            // Cleanup
            FileUtils.deleteDirectory(javaLibraryProject.projectDir)
        }
    }

    private fun ProjectDirProvider.unusedDependenciesFor(spec: LibrarySpec): List<String> {
        return unusedDependenciesFor(spec.name)
    }

    private fun ProjectDirProvider.unusedDependenciesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<UnusedDirectComponent>()
            .map { it.dependency.identifier }
    }

    private fun ProjectDirProvider.completelyUnusedDependenciesFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getUnusedDirectDependenciesPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<UnusedDirectComponent>()
            .filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency.identifier }
    }

    private fun ProjectDirProvider.abiReportFor(moduleName: String): List<String> {
        val module = project(moduleName)
        return module.dir
            .resolve("build/${getAbiAnalysisPath(getVariantOrError(moduleName))}")
            .readText().fromJsonList<Dependency>()
            .map { it.identifier }
    }

    private fun ProjectDirProvider.getVariantOrError(moduleName: String): String {
        return project(moduleName).variant ?: error("No variant associated with module named $moduleName")
    }
}
