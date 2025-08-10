// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path

/**
 * A set of convenience functions for building Gradle projects with
 * [Gradle TestKit](https://docs.gradle.org/current/userguide/test_kit.html). These simplify the creation of
 * [runners][GradleRunner], and building projects with those runners with standard properties, such as forwarding
 * output, enabling stacktraces, and automatically enabling debug mode when `--debug-jvm` is passed.
 */
public object GradleBuilder {

  /** Build the Gradle project defined by [projectDir] and [args]. Expect the build to succeed. */
  @JvmStatic
  public fun build(
    projectDir: Path,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir.toFile(), *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with a custom environment that includes the _current_
   * environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that a custom environment is
   * not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to succeed.
   */
  @JvmStatic
  public fun build(
    projectDir: Path,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir.toFile(), environment, *args)

  /** Build the Gradle project defined by [projectDir] and [args]. Expect the build to succeed. */
  @JvmStatic
  public fun build(
    projectDir: File,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with a custom environment that includes the _current_
   * environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that a custom environment is
   * not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to succeed.
   */
  @JvmStatic
  public fun build(
    projectDir: File,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = build(GradleVersion.current(), projectDir, environment, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion].
   *
   * Expect the build to succeed.
   */
  @JvmStatic
  public fun build(
    gradleVersion: GradleVersion,
    projectDir: Path,
    vararg args: String,
  ): BuildResult = build(gradleVersion, projectDir.toFile(), *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion] and a custom environment that
   * includes the _current_ environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that
   * a custom environment is not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   */
  @JvmStatic
  public fun build(
    gradleVersion: GradleVersion,
    projectDir: Path,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = build(gradleVersion, projectDir.toFile(), environment, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion].
   *
   * Expect the build to succeed.
   */
  @JvmStatic
  public fun build(
    gradleVersion: GradleVersion,
    projectDir: File,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args).build()

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion] and a custom environment that
   * includes the _current_ environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that
   * a custom environment is not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to succeed.
   */
  @JvmStatic
  public fun build(
    gradleVersion: GradleVersion,
    projectDir: File,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args)
    .withEnvironment(System.getenv() + environment)
    .build()

  /** Build the Gradle project defined by [projectDir] and [args]. Expect the build to fail. */
  @JvmStatic
  public fun buildAndFail(
    projectDir: Path,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir.toFile(), *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with a custom environment that includes the _current_
   * environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that a custom environment is
   * not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    projectDir: Path,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir.toFile(), environment, *args)

  /** Build the Gradle project defined by [projectDir] and [args]. Expect the build to fail. */
  @JvmStatic
  public fun buildAndFail(
    projectDir: File,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with a custom environment that includes the _current_
   * environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that a custom environment is
   * not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    projectDir: File,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = buildAndFail(GradleVersion.current(), projectDir, environment, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion].
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: Path,
    vararg args: String,
  ): BuildResult = buildAndFail(gradleVersion, projectDir.toFile(), *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion] and a custom environment that
   * includes the _current_ environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that
   * a custom environment is not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: Path,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = buildAndFail(gradleVersion, projectDir.toFile(), environment, *args)

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion].
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: File,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args).buildAndFail()

  /**
   * Build the Gradle project defined by [projectDir] and [args], with [gradleVersion] and a custom environment that
   * includes the _current_ environment (supplied by [System.getenv]) _plus_ the user-supplied [environment]. Note that
   * a custom environment is not compatible with debug mode (`--debug-jvm`). See
   * [DefaultGradleRunner][org.gradle.testkit.runner.internal.DefaultGradleRunner.run] source for an explanation. Tl;dr:
   * at time of writing, debug mode requires running in-process, but supplying a custom environment requires forking the
   * process, and these two requirements are incompatible.
   *
   * If you want a fully custom environment (no [System.getenv]), use [runner] instead.
   *
   * Expect the build to fail.
   */
  @JvmStatic
  public fun buildAndFail(
    gradleVersion: GradleVersion,
    projectDir: File,
    environment: Map<String, String>,
    vararg args: String,
  ): BuildResult = runner(gradleVersion, projectDir, *args)
    .withEnvironment(System.getenv() + environment)
    .buildAndFail()

  /**
   * Create a runner for the Gradle project defined by [projectDir] and [args], with [gradleVersion]. This allows full
   * customization of the runner before calling either [GradleRunner.build] or [GradleRunner.buildAndFail].
   */
  @JvmStatic
  public fun runner(
    gradleVersion: GradleVersion,
    projectDir: Path,
    vararg args: String,
  ): GradleRunner = runner(gradleVersion, projectDir.toFile(), *args)

  /**
   * Create a runner for the Gradle project defined by [projectDir] and [args], with [gradleVersion]. This allows full
   * customization of the runner before calling either [GradleRunner.build] or [GradleRunner.buildAndFail].
   */
  @JvmStatic
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
    val isDebugJvm = ManagementFactory.getRuntimeMXBean().inputArguments.toString().indexOf("-agentlib:jdwp") > 0
    withDebug(isDebugJvm)
  }
}
