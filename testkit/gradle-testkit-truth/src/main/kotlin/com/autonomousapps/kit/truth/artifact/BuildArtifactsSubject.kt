// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth.artifact

import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.*

// TODO(tsr): what about a new BuildArtifact API? It would wrap Path/File.
public class BuildArtifactsSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: Path?,
) : Subject(failureMetadata, actual) {

  public companion object {
    private val BUILD_ARTIFACT_SUBJECT_FACTORY: Factory<BuildArtifactsSubject, Path> =
      Factory { metadata, actual -> BuildArtifactsSubject(metadata, actual) }

    @JvmStatic
    public fun buildArtifacts(): Factory<BuildArtifactsSubject, Path> = BUILD_ARTIFACT_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: Path?): BuildArtifactsSubject {
      return Truth.assertAbout(buildArtifacts()).that(actual)
    }
  }

  private enum class FileType(val humanReadableName: String) {
    REGULAR_FILE("regular file"),
    DIRECTORY("directory"),
    SYMLINK("symbolic link"),
    UNKNOWN("unknown");

    companion object {
      fun from(path: Path, vararg options: LinkOption): FileType = when {
        path.isRegularFile(*options) -> REGULAR_FILE
        path.isDirectory(*options) -> DIRECTORY
        path.isSymbolicLink() -> SYMLINK
        else -> UNKNOWN
      }
    }
  }

  public fun exists() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }

    check(actual!!.exists())
  }

  public fun notExists() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }

    check(actual!!.notExists())
  }

  public fun isRegularFile() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isRegularFile()) { "Expected regular file. Was ${FileType.from(actual).humanReadableName}" }
  }

  public fun isDirectory() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isDirectory()) { "Expected directory. Was ${FileType.from(actual).humanReadableName}" }
  }

  public fun isSymbolicLink() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }
    check(actual.isSymbolicLink()) { "Expected symbolic link. Was ${FileType.from(actual).humanReadableName}" }
  }

  public fun isJar() {
    isType("jar")
  }

  public fun isType(extension: String) {
    isRegularFile()

    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }

    check(actual.extension == extension) { "Expected extension to be '$extension'. Was '${actual.extension}'" }
  }

  public fun jar(): JarSubject {
    isJar()
    return JarSubject.assertThat(actual)
  }

  public fun file(): PathSubject {
    isRegularFile()

    if (actual == null) {
      failWithActual(Fact.simpleFact("build artifact was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("build artifact does not exist"))
    }

    return PathSubject.assertThat(actual)
  }
}
