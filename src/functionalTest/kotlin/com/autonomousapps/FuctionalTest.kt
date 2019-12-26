package com.autonomousapps

import com.autonomousapps.internal.*
import com.autonomousapps.utils.*
import com.autonomousapps.utils.build
import org.apache.commons.io.FileUtils
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    private lateinit var androidProject: AndroidProject

    private val testMatrix = TestMatrix()

    @Test fun `can execute buildHealth`() {
        testMatrix.forEach { (gradleVersion, agpVersion) ->
            println("Testing against AGP $agpVersion")
            println("Testing against Gradle ${gradleVersion.version}")

            // Given an Android project with an app module and a single android-lib module
            androidProject = AndroidProject(
                agpVersion = agpVersion,
                librarySpecs = listOf(
                    object : LibrarySpec {
                        override val name = "lib"
                        override val type = LibraryType.ANDROID_LIBRARY
                    }
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
            assertTrue { result.output.contains("Task :abiReport") }
            assertTrue { result.output.contains("Task :misusedDependenciesReport") }
            assertTrue { result.output.contains("Task :buildHealth") }

            // Verify unused dependencies reports
            val actualUnusedDepsForApp = completelyUnusedDependenciesFor("app")
            assertTrue { result.output.contains("Completely unused dependencies") }
            assertTrue {
                actualUnusedDepsForApp == listOf(
                    ":lib",
                    "androidx.constraintlayout:constraintlayout",
                    "androidx.core:core-ktx",
                    "androidx.navigation:navigation-fragment-ktx",
                    "androidx.navigation:navigation-ui-ktx",
                    "com.google.android.material:material"
                )
            }

            val actualUnusedDepsForLib = completelyUnusedDependenciesFor("lib")
            assertTrue { actualUnusedDepsForLib == listOf("androidx.constraintlayout:constraintlayout") }

            // Verify ABI reports
            val actualAbi = abiReportFor("lib")
            assertTrue { result.output.contains("These are your API dependencies") }
            assertTrue { actualAbi == listOf("androidx.core:core") }

            // Final result
            assertTrue { result.output.contains("BUILD SUCCESSFUL") }

            // Cleanup
            FileUtils.deleteDirectory(androidProject.projectDir)
        }
    }

    // TODO maybe push these down into the AndroidProject class itself
    private fun completelyUnusedDependenciesFor(moduleName: String): List<String> {
        return androidProject.project(moduleName).dir
            .resolve("build/${getUnusedDirectDependenciesPath("debug")}")
            .readText().fromJsonList<UnusedDirectComponent>()
            .filter { it.usedTransitiveDependencies.isEmpty() }
            .map { it.dependency.identifier }
    }

    private fun abiReportFor(moduleName: String): List<String> {
        return androidProject.project(moduleName).dir
            .resolve("build/${getAbiAnalysisPath("debug")}")
            .readText().fromJsonList<Dependency>()
            .map { it.identifier }
    }
}
