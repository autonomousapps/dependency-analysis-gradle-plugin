@file:JvmName("Runner")

package com.autonomousapps.utils

import com.autonomousapps.fixtures.ProjectDirProvider
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File

// For better Groovy support
internal fun build(
  gradleVersion: Any,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion as GradleVersion, projectDirProvider.projectDir, *args).build()

internal fun build(
  gradleVersion: GradleVersion,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion, projectDirProvider.projectDir, *args).build()

internal fun build(
  gradleVersion: GradleVersion,
  projectDir: File,
  vararg args: String
) = runner(gradleVersion, projectDir, *args).build()

// For better Groovy support
internal fun buildAndFail(
  gradleVersion: Any,
  projectDir: File,
  vararg args: String
) = runner(gradleVersion as GradleVersion, projectDir, *args).buildAndFail()

// For better Groovy support
internal fun buildAndFail(
  gradleVersion: Any,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion as GradleVersion, projectDirProvider.projectDir, *args).buildAndFail()

internal fun buildAndFail(
  gradleVersion: GradleVersion,
  projectDirProvider: ProjectDirProvider,
  vararg args: String
) = runner(gradleVersion, projectDirProvider.projectDir, *args).buildAndFail()

internal fun runner(
  gradleVersion: GradleVersion,
  projectDir: File,
  vararg args: String
): GradleRunner = GradleRunner.create().apply {
  forwardOutput()
  withGradleVersion(gradleVersion.version)
  withProjectDir(projectDir)
  withArguments(args.toList() + "-s")
  //withDebug(true)
}
