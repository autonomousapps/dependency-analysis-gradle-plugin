package com.autonomousapps

import com.autonomousapps.fixtures.*
import com.autonomousapps.utils.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("FunctionName")
class AndroidFunctionalTests : AbstractFunctionalTests() {

    @Test fun `advice filters work`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project that needs comprehensive "advice"
            val extension = """
                |dependencyAnalysis {
                |    issues {
                |       onAny {
                |           fail("$KOTLIN_STDLIB_JDK7_ID")
                |       }
                |       onUnusedDependencies {
                |           fail(":lib_android")
                |       }
                |       onUsedTransitiveDependencies {
                |           fail("$CORE_ID")
                |       }
                |       onIncorrectConfiguration {
                |           fail("$COMMONS_COLLECTIONS_ID")
                |       }
                |    }
                |}
                """.trimMargin()
            val androidProject = androidProjectThatNeedsAdvice(agpVersion = agpVersion, extensionSpec = extension)

            // When
            val result = buildAndFail(gradleVersion, androidProject, "buildHealth")

            // Then
            // ...core tasks ran and were successful
            result.task(":buildHealth")?.outcome.assertSuccess()
            result.task(":failOrWarn")?.outcome.assertFailed()
            result.task(":app:adviceDebug")?.outcome.assertSuccess()
            result.task(":lib_android:adviceDebug")?.outcome.assertSuccess()
            result.task(":lib_jvm:adviceMain")?.outcome.assertSuccess()

            // ...reports are as expected
            // ...for app
            val expectedAppAdvice = expectedAppAdvice(ignore = setOf(KOTLIN_STDLIB_JDK7_ID, ":lib_android"))
            val actualAppAdvice = androidProject.adviceFor("app")
            assertEquals(
                expectedAppAdvice, actualAppAdvice,
                "\nExpected $expectedAppAdvice\nActual   $actualAppAdvice\n"
            )

            // ...for lib_android
            val expectedLibAndroidAdvice = expectedLibAndroidAdvice(ignore = setOf(KOTLIN_STDLIB_JDK7_ID, CORE_ID))
            val actualLibAndroidAdvice = androidProject.adviceFor("lib_android")
            assertEquals(
                expectedLibAndroidAdvice, actualLibAndroidAdvice,
                "\nExpected $expectedLibAndroidAdvice\nActual   $actualLibAndroidAdvice\n"
            )

            // ...for lib_jvm
            val expectedLibJvmAdvice = expectedLibJvmAdvice(ignore = setOf(KOTLIN_STDLIB_JDK7_ID, COMMONS_COLLECTIONS_ID))
            val actualLibJvmAdvice = androidProject.adviceFor("lib_jvm")
            assertEquals(
                expectedLibJvmAdvice, actualLibJvmAdvice,
                "\nExpected $expectedLibJvmAdvice\nActual   $actualLibJvmAdvice\n"
            )

            cleanup(androidProject)
        }
    }

    @Test fun `accurate advice can be given`() {
        testMatrix.forEachPrinting { (gradleVersion, agpVersion) ->
            // Given an Android project that needs comprehensive "advice"
            val androidProject = androidProjectThatNeedsAdvice(agpVersion)

            // When
            val result = build(gradleVersion, androidProject, "buildHealth")

            // Then
            // ...core tasks ran and were successful
            result.task(":buildHealth")?.outcome.assertSuccess()
            result.task(":app:adviceDebug")?.outcome.assertSuccess()
            result.task(":lib_android:adviceDebug")?.outcome.assertSuccess()
            result.task(":lib_jvm:adviceMain")?.outcome.assertSuccess()

            // ...reports are as expected
            // ...for app
            val expectedAppAdvice = expectedAppAdvice()
            val actualAppAdvice = androidProject.adviceFor("app")
            assertEquals(
                expectedAppAdvice, actualAppAdvice,
                "\nExpected $expectedAppAdvice\nActual   $actualAppAdvice\n"
            )

            // ...for lib_android
            val expectedLibAndroidAdvice = expectedLibAndroidAdvice()
            val actualLibAndroidAdvice = androidProject.adviceFor("lib_android")
            assertEquals(
                expectedLibAndroidAdvice, actualLibAndroidAdvice,
                "\nExpected $expectedLibAndroidAdvice\nActual   $actualLibAndroidAdvice\n"
            )

            // ...for lib_jvm
            val expectedLibJvmAdvice = expectedLibJvmAdvice()
            val actualLibJvmAdvice = androidProject.adviceFor("lib_jvm")
            assertEquals(
                expectedLibJvmAdvice, actualLibJvmAdvice,
                "\nExpected $expectedLibJvmAdvice\nActual   $actualLibJvmAdvice\n"
            )

            cleanup(androidProject)
        }
    }

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

    @Test fun `buildHealth can be executed`() {
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
            result.task(":adviceReport")?.outcome.assertSuccess()
            result.task(":buildHealth")?.outcome.assertSuccess()

            // ...in the app project?
            result.task(":app:misusedDependenciesDebug")?.outcome.assertSuccess()
            result.task(":app:adviceDebug")?.outcome.assertSuccess()

            // ...in the lib project?
            result.task(":lib:misusedDependenciesDebug")?.outcome.assertSuccess()
            result.task(":lib:abiAnalysisDebug")?.outcome.assertSuccess()
            result.task(":lib:adviceDebug")?.outcome.assertSuccess()

            // Verify unused dependencies reports
            val actualUnusedDepsForApp = androidProject.completelyUnusedDependenciesFor("app")
            val expectedUnusedDepsForApp = listOf(
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
                    "implementation" to APPCOMPAT
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

