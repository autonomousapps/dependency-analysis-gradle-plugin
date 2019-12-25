package com.autonomousapps.utils

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

internal fun build(
    gradleVersion: GradleVersion,
    projectDirProvider: ProjectDirProvider,
    vararg args: String
) = GradleRunner.create().apply {
    forwardOutput()
    withPluginClasspath()
    withGradleVersion(gradleVersion.version)
    withProjectDir(projectDirProvider.projectDir)
    withArguments(*args)
}.build()

internal fun AndroidProject.build(vararg args: String) = runner(this, *args).build()

internal fun AndroidProject.buildAndFail(vararg args: String) = runner(this, *args).buildAndFail()

internal fun runner(androidProject: AndroidProject, vararg args: String) = GradleRunner.create().apply {
    forwardOutput()
    withPluginClasspath()
    withArguments(*args)
    withProjectDir(androidProject.projectDir)
}
