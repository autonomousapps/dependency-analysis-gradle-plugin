package com.autonomousapps.utils

import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import kotlin.test.assertTrue

internal fun build(
    gradleVersion: GradleVersion,
    projectDirProvider: ProjectDirProvider,
    vararg args: String
) = runner(gradleVersion, projectDirProvider, *args).build()

internal fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDirProvider: ProjectDirProvider,
    vararg args: String
) = runner(gradleVersion, projectDirProvider, *args).buildAndFail()

internal fun runner(
    gradleVersion: GradleVersion,
    projectDirProvider: ProjectDirProvider,
    vararg args: String
) = GradleRunner.create().apply {
    forwardOutput()
    withPluginClasspath()
    withGradleVersion(gradleVersion.version)
    withProjectDir(projectDirProvider.projectDir)
    withArguments(*args)
//    withDebug(true)
}

internal fun TaskOutcome?.assertSuccess() {
    assertTrue("Expected SUCCESS\nActual  $this") {
        TaskOutcome.SUCCESS == this
    }
}

internal fun TaskOutcome?.assertFailed() {
    assertTrue("Expected FAILED\nActual  $this") {
        TaskOutcome.FAILED == this
    }
}
