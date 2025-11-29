// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth.artifact

import com.autonomousapps.kit.truth.AbstractSubject
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Truth
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.notExists

public class JarSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: Path?,
) : AbstractSubject<Path>(failureMetadata, actual) {

  public companion object {
    private val JAR_SUBJECT_FACTORY: Factory<JarSubject, Path> =
      Factory { metadata, actual -> JarSubject(metadata, actual) }

    @JvmStatic
    public fun jars(): Factory<JarSubject, Path> = JAR_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: Path?): JarSubject = Truth.assertAbout(jars()).that(actual)
  }

  public fun containsResource(path: String) {
    resource(path).exists()
  }

  public fun resource(path: String): PathSubject {
    val actual = assertNonNull(actual) { "jar was null" }

    // Open zip, copy entry to temp dir, close zip.
    val tempResource = FileSystems.newFileSystem(actual, null).use { fs ->
      val resource = fs.getPath(path)
      if (resource.notExists()) {
        failWithActual(Fact.simpleFact("No resource found at '$path' in '$actual'"))
      }

      val tempDir = Files.createTempDirectory(null)
      Files.copy(resource, tempDir.resolve(path), StandardCopyOption.REPLACE_EXISTING)
    }

    return PathSubject.assertThat(tempResource)
  }
}
