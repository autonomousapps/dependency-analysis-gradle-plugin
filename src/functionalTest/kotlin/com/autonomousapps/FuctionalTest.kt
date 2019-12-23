package com.autonomousapps

import com.autonomousapps.utils.AndroidProject
import com.autonomousapps.utils.build
import org.gradle.testkit.runner.BuildResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    lateinit var androidProject: AndroidProject

    @AfterTest fun cleanup() {
        androidProject.projectDir.delete()
    }

    @Test fun `can assemble app`() {
        androidProject = AndroidProject()

        // Run the build
        val result = androidProject.build("app:assembleDebug")

        // Verify the result
        assertTrue { result.output.contains("Task :app:assembleDebug") }
        assertTrue { result.output.contains("BUILD SUCCESSFUL") }
    }

    @Test fun `can execute buildHealth`() {
        androidProject = AndroidProject(listOf("lib"))

        // Run the build
        val result = androidProject.build("buildHealth", "--rerun-tasks")

        // Verify the result
        // Aggregate tasks
        assertTrue { result.output.contains("Task :abiReport") }
        assertTrue { result.output.contains("Task :misusedDependenciesReport") }
        assertTrue { result.output.contains("Task :buildHealth") }
        // Reports
        assertTrue {
            result.hasUnusedDependencies(listOf(
                ":lib",
                "androidx.constraintlayout:constraintlayout",
                "androidx.core:core-ktx",
                "androidx.navigation:navigation-fragment-ktx",
                "androidx.navigation:navigation-ui-ktx",
                "com.google.android.material:material"
            ))
        }

        assertTrue {
            result.hasApiDependencies(listOf(
                    "androidx.core:core"
            ))
        }

        // Final result
        assertTrue { result.output.contains("BUILD SUCCESSFUL") }
    }

    // TODO the format here is hardcoded. Would be preferable to make it a bit more flexible
    private fun BuildResult.hasUnusedDependencies(deps: List<String>) = output.contains("""
        |Completely unused dependencies:
        |${deps.joinToString(prefix = "- ", separator = "\n- ")}
    """.trimMargin("|"))

    // TODO the format here is hardcoded. Would be preferable to make it a bit more flexible
    private fun BuildResult.hasApiDependencies(deps: List<String>) = output.contains("""
        |These are your API dependencies:
        |${deps.joinToString(prefix = "- ", separator = "\n- ")}
    """.trimMargin("|"))
}
