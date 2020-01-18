package com.autonomousapps

import com.autonomousapps.fixtures.*
import com.autonomousapps.utils.assertSuccess
import com.autonomousapps.utils.build
import com.autonomousapps.utils.forEachPrinting
import org.apache.commons.io.FileUtils
import kotlin.test.Test
import kotlin.test.assertTrue

@Suppress("FunctionName")
class AndroidFunctionalTests : AbstractFunctionalTests() {

    @Test fun `plugin accounts for android resource usage`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project with an app module and a lib module. The app module only uses a resource from the
            // lib module
            val androidProject = androidProjectUsingResourcesOnly(agpVersion)

            // When
            val result = build(gradleVersion, androidProject, "buildHealth")

            // Then
            result.task(":buildHealth")?.outcome.assertSuccess()

            val actualUnusedDepsForApp = androidProject.unusedDependenciesFor("app")
            val expectedUnusedDepsForApp = listOf(
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7"
            )
            assertTrue("Actual unused app dependencies: $actualUnusedDepsForApp\nExpected: $expectedUnusedDepsForApp") {
                expectedUnusedDepsForApp == actualUnusedDepsForApp
            }

            cleanup(androidProject)
        }
    }

    @Test fun `can execute buildHealth`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project with an app module and a single android-lib module
            val androidProject = defaultAndroidProject(agpVersion)

            // When
            val result = build(gradleVersion, androidProject, "buildHealth")

            // Then
            // Did expected tasks run?
            // ...in the root project?
            result.task(":abiReport")?.outcome.assertSuccess()
            result.task(":misusedDependenciesReport")?.outcome.assertSuccess()
            result.task(":buildHealth")?.outcome.assertSuccess()

            // ...in the app project?
            result.task(":app:misusedDependenciesDebug")?.outcome.assertSuccess()

            // ...in the lib project?
            result.task(":lib:misusedDependenciesDebug")?.outcome.assertSuccess()
            result.task(":lib:abiAnalysisDebug")?.outcome.assertSuccess()

            // Verify unused dependencies reports
            val actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor("app")
            val expectedUnusedDepsForApp = listOf(
                ":java_lib",
                "androidx.constraintlayout:constraintlayout",
                "com.google.android.material:material"
            )
            assertTrue("Actual unused app dependencies: $actualUnusedDepsForApp\nExpected: $expectedUnusedDepsForApp") {
                expectedUnusedDepsForApp == actualUnusedDepsForApp
            }

            val actualUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor("lib")
            val expectedUnusedDepsForLib = listOf("androidx.constraintlayout:constraintlayout")
            assertTrue { expectedUnusedDepsForLib == actualUnusedDepsForLib }

            // Verify ABI reports
            val actualAbi = androidProject.abiReportFor("lib")
            val expectedAbi = listOf("androidx.core:core")
            assertTrue { expectedAbi == actualAbi }

            cleanup(androidProject)
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
            val result = build(gradleVersion, androidProject, "buildHealth")

            // Then
            // Did expected tasks run?
            // ...in the lib project?
            result.task(":$libName:misusedDependenciesDebug")?.outcome.assertSuccess()
            result.task(":$libName:abiAnalysisDebug")?.outcome.assertSuccess()

            // Verify unused dependencies reports
            val actualCompletelyUnusedDepsForLib = androidProject.completelyUnusedDependenciesFor(libName)
            assertTrue("Expected an empty list, got $actualCompletelyUnusedDepsForLib") {
                emptyList<String>() == actualCompletelyUnusedDepsForLib
            }

            val actualUnusedDependencies = androidProject.unusedDependenciesFor(libName)
            assertTrue("Expected an empty list, got $actualUnusedDependencies") {
                emptyList<String>() == actualUnusedDependencies
            }

            // Verify ABI reports
            val actualAbi = androidProject.abiReportFor(libName)
            assertTrue("Expected an empty list, got $actualAbi") {
                emptyList<String>() == actualAbi
            }

            cleanup(androidProject)
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

            cleanup(javaLibraryProject)
        }
    }

    private fun cleanup(projectDirProvider: ProjectDirProvider) {
        FileUtils.deleteDirectory(projectDirProvider.projectDir)
    }

    private fun defaultAndroidProject(agpVersion: String) = AndroidProject(
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

    // In this app project, the only reference to the lib project is through a color resource.
    // Does the plugin correctly say that 'lib' is a used dependency?
    private fun androidProjectUsingResourcesOnly(agpVersion: String): AndroidProject {
        val libName = "lib"
        return AndroidProject(
            agpVersion = agpVersion,
            appSpec = AppSpec(
                sources = mapOf("MainActivity.kt" to """
                    package $DEFAULT_PACKAGE_NAME
                    
                    import androidx.appcompat.app.AppCompatActivity
                    import $DEFAULT_PACKAGE_NAME.$libName.R
                                    
                    class MainActivity : AppCompatActivity() {
                        val i = R.color.libColor
                    }
                """.trimIndent()),
                dependencies = DEPENDENCIES_KOTLIN_STDLIB + listOf(
                    "implementation" to "androidx.appcompat:appcompat:1.1.0"
                )
            ),
            librarySpecs = listOf(
                LibrarySpec(
                    name = libName,
                    type = LibraryType.KOTLIN_ANDROID,
                    sources = emptyMap(),
                    dependencies = DEPENDENCIES_KOTLIN_STDLIB
                )
            )
        )
    }
}
