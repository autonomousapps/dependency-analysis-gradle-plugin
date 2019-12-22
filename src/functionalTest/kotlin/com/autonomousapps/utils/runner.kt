package com.autonomousapps.utils

import org.gradle.testkit.runner.GradleRunner

internal fun AndroidProject.build(vararg args: String) = runner(this, *args).build()

internal fun AndroidProject.buildAndFail(vararg args: String) = runner(this, *args).buildAndFail()

private fun runner(androidProject: AndroidProject, vararg args: String) = GradleRunner.create().apply {
    forwardOutput()
    withPluginClasspath()
    withArguments(*args)
    withProjectDir(androidProject.projectDir)
}
