@file:JvmName("Runner")

package com.autonomousapps.utils

import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion

// For better Groovy support
internal fun build(
  gradleVersion: Any,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion as GradleVersion, projectDirProvider, *args).build()

internal fun build(
  gradleVersion: GradleVersion,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion, projectDirProvider, *args).build()

// For better Groovy support
internal fun buildAndFail(
  gradleVersion: Any,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion as GradleVersion, projectDirProvider, *args).buildAndFail()

internal fun buildAndFail(
  gradleVersion: GradleVersion,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion, projectDirProvider, *args).buildAndFail()

internal fun runner(
  gradleVersion: GradleVersion,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
): GradleRunner = GradleRunner.create().apply {
  forwardOutput()
  withGradleVersion(gradleVersion.version)
  withProjectDir(projectDirProvider.projectDir)
  withArguments(args.toList() + "-s")
  //withDebug(true)
}
