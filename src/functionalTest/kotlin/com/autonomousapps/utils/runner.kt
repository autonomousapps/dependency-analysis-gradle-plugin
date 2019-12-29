package com.autonomousapps.utils

import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

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
}
