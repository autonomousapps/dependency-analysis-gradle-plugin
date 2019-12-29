package com.autonomousapps

import com.autonomousapps.fixtures.AndroidProject
import com.autonomousapps.fixtures.LibrarySpec
import com.autonomousapps.fixtures.LibraryType
import com.autonomousapps.fixtures.WORKSPACE
import com.autonomousapps.internal.*
import com.autonomousapps.utils.*
import com.autonomousapps.utils.build
import org.apache.commons.io.FileUtils
import org.gradle.util.GradleVersion
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class FunctionalTest {

    private lateinit var androidProject: AndroidProject

    private val testMatrix = TestMatrix()

    @BeforeTest fun cleanWorkspace() {
        // Same as androidProject.projectDir, but androidProject has not been instantiated yet
        FileUtils.deleteDirectory(File(WORKSPACE))
    }

    @Test fun `core ktx is a direct dependency`() {
        // Given an Android project with an app module and a single android-lib module
        androidProject = AndroidProject(
            agpVersion = "3.5.3",
            librarySpecs = listOf(
                LibrarySpec(
                    name = "lib",
                    type = LibraryType.KOTLIN_ANDROID,
                    dependencies = listOf(
                        "implementation" to "androidx.core:core-ktx:1.1.0"
                    ),
                    sources = mapOf(
                        "CoreKtxLibrary.kt" to """
                            import android.content.Context
                            import android.text.SpannableStringBuilder 
                            import androidx.core.content.ContextCompat
                            import androidx.core.text.bold
                            import androidx.core.text.color
                            import com.autonomousapps.test.lib.R

                            class CoreKtxLibrary {
                                fun useCoreKtx(context: Context): CharSequence {
                                    return SpannableStringBuilder("just some text")
                                        .bold {
                                            color(ContextCompat.getColor(context, R.color.colorAccent)) { append("some more text") }
                                        }
                                }
                            }
                        """.trimIndent()
                    )
                )
            )
        )

        // When
        val result = build(
            GradleVersion.version("5.6.4"),
            androidProject,
            "buildHealth"
        )

        // Then

    }

    @Test fun `can execute buildHealth`() {
        testMatrix.forEach { (gradleVersion, agpVersion) ->
            println("Testing against AGP $agpVersion")
            println("Testing against Gradle ${gradleVersion.version}")

            // Given an Android project with an app module and a single android-lib module
            androidProject = AndroidProject(
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
            val actualUnusedDepsForApp = completelyUnusedDependenciesFor("app")
            assertTrue { result.output.contains("Completely unused dependencies") }
            assertTrue {
                actualUnusedDepsForApp == listOf(
                    ":java_lib",
                    ":kotlin_lib",
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
