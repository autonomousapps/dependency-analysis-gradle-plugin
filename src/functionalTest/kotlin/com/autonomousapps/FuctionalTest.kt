package com.autonomousapps

import com.autonomousapps.models.Dependency
import com.autonomousapps.models.UnusedDirectComponent
import com.autonomousapps.utils.AndroidProject
import com.autonomousapps.utils.TestMatrix
import com.autonomousapps.utils.build
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    private lateinit var androidProject: AndroidProject

    private val testMatrix = TestMatrix()

    @AfterTest fun cleanup() {
        androidProject.projectDir.delete()
    }

    @Test fun `can execute buildHealth`() {
        testMatrix.forEach { (gradleVersion, agpVersion) ->
            println("Testing against AGP $agpVersion")
            println("Testing against Gradle ${gradleVersion.version}")

            androidProject = AndroidProject(
                agpVersion = agpVersion,
                libraries = listOf("lib")
            )

            val result = build(
                gradleVersion,
                androidProject,
                "buildHealth", "--rerun-tasks"
            )

            // Verify the result
            // Aggregate tasks
            assertTrue { result.output.contains("Task :abiReport") }
            assertTrue { result.output.contains("Task :misusedDependenciesReport") }
            assertTrue { result.output.contains("Task :buildHealth") }

            // Reports
            val actualCompletelyUnusedDeps = androidProject.appProject.appDir
                .resolve("build/${getUnusedDirectDependenciesPath("debug")}")
                .readText().fromJsonList<UnusedDirectComponent>()
                .filter { it.usedTransitiveDependencies.isEmpty() }
                .map { it.dependency.identifier }

            assertTrue { result.output.contains("Completely unused dependencies") }
            assertTrue {
                actualCompletelyUnusedDeps == listOf(
                    ":lib",
                    "androidx.constraintlayout:constraintlayout",
                    "androidx.core:core-ktx",
                    "androidx.navigation:navigation-fragment-ktx",
                    "androidx.navigation:navigation-ui-ktx",
                    "com.google.android.material:material"
                )
            }

            val actualAbi = androidProject.libProjects.first().libDir
                .resolve("build/${getAbiAnalysisPath("debug")}")
                .readText().fromJsonList<Dependency>()
                .map { it.identifier }

            assertTrue { result.output.contains("These are your API dependencies") }
            assertTrue {
                actualAbi == listOf("androidx.core:core")
            }

            // Final result
            assertTrue { result.output.contains("BUILD SUCCESSFUL") }
        }
    }
}
