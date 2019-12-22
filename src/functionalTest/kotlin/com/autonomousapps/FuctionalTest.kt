package com.autonomousapps

import com.autonomousapps.utils.AndroidProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    @Test fun `can assemble app`() {
        // Setup the test build
        val androidProject = AndroidProject()

        // Run the build
        val result = GradleRunner.create().apply {
            forwardOutput()
            withPluginClasspath()
            withArguments("app:assembleDebug")
            withProjectDir(androidProject.projectDir)
        }.build()

        // Verify the result
        assertTrue { result.output.contains("Task :app:assembleDebug") }
        assertTrue { result.output.contains("BUILD SUCCESSFUL") }
    }

    @Test fun `can execute buildHealth`() {
        // Setup the test build
        val androidProject = AndroidProject()

        // Run the build
        val result = GradleRunner.create().apply {
            forwardOutput()
            withPluginClasspath()
            withArguments("buildHealth", "--rerun-tasks")
            withProjectDir(androidProject.projectDir)
        }.build()

        // Verify the result
        // Aggregate tasks
        assertTrue { result.output.contains("Task :abiReport") }
        assertTrue { result.output.contains("Task :misusedDependenciesReport") }
        assertTrue { result.output.contains("Task :buildHealth") }
        // Reports
        assertTrue {
            result.hasUnusedDependencies(listOf(
                "androidx.constraintlayout:constraintlayout",
                "androidx.core:core-ktx",
                "androidx.navigation:navigation-fragment-ktx",
                "androidx.navigation:navigation-ui-ktx",
                "com.google.android.material:material"
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
}
