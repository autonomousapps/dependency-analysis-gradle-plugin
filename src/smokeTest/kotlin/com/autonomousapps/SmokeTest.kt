@file:Suppress("FunctionName")

package com.autonomousapps

import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

private const val WORKSPACE = "build/smokeTest"

class SmokeTest {

    private var simpleProjectDir: File? = null

    @After fun cleanup() {
        simpleProjectDir?.let { FileUtils.deleteDirectory(it) }
    }

    // This will catch the case that led to this commit: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/commit/ffe4d30302a73acab2dfd259b20998f3604b5963
    // Had to revert modularization because Gradle couldn't resolve project dependency. Tests passed locally, but plugin
    // could not be used by normal consumers.
    @Test fun `binary plugin can be applied`() {
        val projectVersion = System.getProperty("com.autonomousapps.version")
        System.err.println("Testing version $projectVersion")
        simpleProjectDir = simpleProject(projectVersion)

        val result = GradleRunner.create().apply {
            forwardOutput()
            withPluginClasspath()
            withGradleVersion("6.0.1")
            withProjectDir(simpleProjectDir)
            withArguments("help")
        }.build()

        assertTrue("I guess build wasn't successful") {
            result.output.contains("BUILD SUCCESSFUL")
        }
    }

    private fun simpleProject(projectVersion: String): File {
        val rootDir = File(WORKSPACE)
        rootDir.mkdirs()

        val buildSrc = rootDir.resolve("buildSrc")
        buildSrc.mkdirs()
        buildSrc.resolve("settings.gradle").writeText("")
        buildSrc.resolve("build.gradle").writeText("""
            repositories {
                gradlePluginPortal()
                jcenter()
            }
            dependencies {
                // This forces download of the actual binary plugin, rather than using what is bundled with project
                implementation "gradle.plugin.com.autonomousapps:dependency-analysis-gradle-plugin:${projectVersion}"
            }
        """.trimIndent())

        rootDir.resolve("settings.gradle").writeText("""
            rootProject.name = 'smoke-test'
        """.trimIndent())

        rootDir.resolve("build.gradle").writeText("""
            plugins {
                id 'com.autonomousapps.dependency-analysis'
            }
            repositories {
                jcenter()
            }
        """.trimIndent())

        return rootDir
    }
}
