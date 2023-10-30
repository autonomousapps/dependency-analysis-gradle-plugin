package com.autonomousapps.kit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path

public object GradleBuilder {

  public fun build(
    projectDir: Path,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir.toFile(), *args)

  public fun build(
    projectDir: File,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir, *args)

  public fun build(
    gradleVersion: GradleVersion,
    projectDir: Path,
    vararg args: String,
  ): BuildResult = build(gradleVersion, projectDir.toFile(), *args)

  public fun build(
    gradleVersion: GradleVersion,
    projectDir: File,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args).build()

  public fun buildAndFail(
    projectDir: Path,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir.toFile(), *args)

  public fun buildAndFail(
    projectDir: File,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir, *args)

  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: Path,
    vararg args: String,
  ): BuildResult = buildAndFail(gradleVersion, projectDir.toFile(), *args)

  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: File,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args).buildAndFail()

  public fun runner(
    gradleVersion: GradleVersion,
    projectDir: File,
    vararg args: String,
  ): GradleRunner = GradleRunner.create().apply {
    forwardOutput()
    withGradleVersion(gradleVersion.version)
    withProjectDir(projectDir)
    withArguments(args.toList() + "-s")
    // Ensure this value is true when `--debug-jvm` is passed to Gradle, and false otherwise
    withDebug(
      ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0
    )
  }
}
