@file:Suppress("FunctionName")

package com.autonomousapps

import com.autonomousapps.fixtures.MultiModuleJavaProject
import com.autonomousapps.fixtures.newSimpleProject
import org.apache.commons.io.FileUtils
import org.gradle.api.logging.Logging
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.After
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

const val WORKSPACE = "build/smokeTest"

class SmokeTest {

    private val logger = Logging.getLogger(SmokeTest::class.java)

    private lateinit var theProjectDir: File
    private val projectVersion = System.getProperty("com.autonomousapps.version").also {
        logger.quiet("Testing version $it")
    }

    @After fun cleanup() {
        FileUtils.deleteDirectory(theProjectDir)
    }

    // This will catch the case that led to this commit: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/commit/ffe4d30302a73acab2dfd259b20998f3604b5963
    // Had to revert modularization because Gradle couldn't resolve project dependency. Tests passed locally, but plugin
    // could not be used by normal consumers.
    @Test fun `binary plugin can be applied`() {
        theProjectDir = newSimpleProject(projectVersion)

        val result = GradleRunner.create().apply {
            forwardOutput()
            withPluginClasspath()
            withGradleVersion(GradleVersion.current().version)
            withProjectDir(theProjectDir)
            withArguments("help")
        }.build()

        assertTrue("I guess build wasn't successful") {
            result.output.contains("BUILD SUCCESSFUL")
        }
    }

    // This will catch the case where there's a runtime issue
    @Test fun `can execute buildHealth in a multi-module Java project with custom variants`() {
        val project = MultiModuleJavaProject(
            projectVersion = projectVersion,
            extension = """
                dependencyAnalysis {
                    // this is what triggered the issue
                    setVariants("main")
                }
            """.trimIndent()
        )
        theProjectDir = project.rootDir

        val result = GradleRunner.create().apply {
            forwardOutput()
            withPluginClasspath()
            withGradleVersion(GradleVersion.current().version)
            withProjectDir(theProjectDir)
            withArguments("buildHealth")
        }.build()

        assertTrue("I guess build wasn't successful") {
            result.output.contains("BUILD SUCCESSFUL")
        }
    }
}
