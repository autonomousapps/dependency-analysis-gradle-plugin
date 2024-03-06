// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth.artifact

import com.autonomousapps.kit.truth.AbstractSubject
import com.google.common.truth.*
import com.google.common.truth.Subject.Factory
import com.google.errorprone.annotations.CanIgnoreReturnValue
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.readText

public class PathSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: Path?,
) : AbstractSubject<Path>(failureMetadata, actual) {

  public companion object {
    private val PATH_SUBJECT_FACTORY: Factory<PathSubject, Path> =
      Factory { metadata, actual -> PathSubject(metadata, actual) }

    @JvmStatic
    public fun paths(): Factory<PathSubject, Path> = PATH_SUBJECT_FACTORY

    @JvmStatic
    public fun assertThat(actual: Path?): PathSubject {
      return Truth.assertAbout(paths()).that(actual)
    }
  }

  @CanIgnoreReturnValue
  public fun exists(): Path {
    val actual = assertNonNull(actual) { "path was null" }
    if (actual.notExists()) {
      failWithActual(Fact.simpleFact("path does not exist"))
    }

    return actual
  }

  @CanIgnoreReturnValue
  public fun notExists(): Path {
    val actual = assertNonNull(actual) { "path was null" }
    if (actual.exists()) {
      failWithActual(Fact.simpleFact("path exists"))
    }

    return actual
  }

  @JvmOverloads
  public fun text(charset: Charset = Charsets.UTF_8): StringSubject {
    val actual = assertNonNull(actual) { "path was null" }
    return check("readText()").that(actual.readText(charset))
  }

  @JvmOverloads
  public fun lines(charset: Charset = Charsets.UTF_8): IterableSubject {
    val actual = assertNonNull(actual) { "path was null" }
    return check("readLines()").that(actual.readLines(charset))
  }
}
