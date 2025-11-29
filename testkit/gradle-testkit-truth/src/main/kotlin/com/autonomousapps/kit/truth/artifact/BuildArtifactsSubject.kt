// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth.artifact

import com.autonomousapps.kit.artifacts.BuildArtifact
import com.autonomousapps.kit.artifacts.FileType
import com.autonomousapps.kit.truth.AbstractSubject
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Truth
import com.google.errorprone.annotations.CanIgnoreReturnValue

public class BuildArtifactsSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: BuildArtifact?,
) : AbstractSubject<BuildArtifact>(failureMetadata, actual) {

  public companion object {
    private val BUILD_ARTIFACT_SUBJECT_FACTORY: Factory<BuildArtifactsSubject, BuildArtifact> =
      Factory { metadata, actual -> BuildArtifactsSubject(metadata, actual) }

    @JvmStatic
    public fun buildArtifacts(): Factory<BuildArtifactsSubject, BuildArtifact> = BUILD_ARTIFACT_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: BuildArtifact?): BuildArtifactsSubject {
      return Truth.assertAbout(buildArtifacts()).that(actual)
    }
  }

  @CanIgnoreReturnValue
  public fun exists(): BuildArtifact {
    val actual = assertNonNull(actual) { "build artifact was null" }
    check(actual.exists())

    return actual
  }

  @CanIgnoreReturnValue
  public fun notExists(): BuildArtifact {
    val actual = assertNonNull(actual) { "build artifact was null" }
    check(actual.notExists())

    return actual
  }

  @CanIgnoreReturnValue
  public fun isRegularFile(): BuildArtifact {
    val actual = assertNonNull(actual) { "build artifact was null" }
    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isRegularFile()) { "Expected regular file. Was ${FileType.from(actual).humanReadableName}" }

    return actual
  }

  @CanIgnoreReturnValue
  public fun isDirectory(): BuildArtifact {
    val actual = assertNonNull(actual) { "build artifact was null" }
    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isDirectory()) { "Expected directory. Was ${FileType.from(actual).humanReadableName}" }

    return actual
  }

  @CanIgnoreReturnValue
  public fun isSymbolicLink(): BuildArtifact {
    val actual = assertNonNull(actual) { "build artifact was null" }
    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isSymbolicLink()) { "Expected symbolic link. Was ${FileType.from(actual).humanReadableName}" }

    return actual
  }

  @CanIgnoreReturnValue
  public fun isJar(): BuildArtifact = isType("jar")

  @CanIgnoreReturnValue
  public fun isType(extension: String): BuildArtifact {
    isRegularFile()

    val actual = assertNonNull(actual) { "build artifact was null" }
    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }

    check(actual.extension == extension) { "Expected extension to be '$extension'. Was '${actual.extension}'" }

    return actual
  }

  public fun jar(): JarSubject {
    val actual = isJar()
    return JarSubject.assertThat(actual.asPath)
  }

  public fun file(): PathSubject {
    val actual = isRegularFile()

    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }

    return PathSubject.assertThat(actual.asPath)
  }
}
