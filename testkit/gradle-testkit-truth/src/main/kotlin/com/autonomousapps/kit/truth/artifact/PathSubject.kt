// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.truth.artifact

import com.google.common.truth.*
import com.google.common.truth.Subject.Factory
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.readText

public class PathSubject private constructor(
  failureMetadata: FailureMetadata,
  private val actual: Path?,
) : Subject(failureMetadata, actual) {

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

  public fun exists() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("path was null"))
    }
    if (actual!!.notExists()) {
      failWithActual(Fact.simpleFact("path does not exist"))
    }
  }

  public fun notExists() {
    if (actual == null) {
      failWithActual(Fact.simpleFact("path was null"))
    }
    if (actual!!.exists()) {
      failWithActual(Fact.simpleFact("path exists"))
    }
  }

  @JvmOverloads
  public fun text(charset: Charset = Charsets.UTF_8): StringSubject {
    if (actual == null) {
      failWithActual(Fact.simpleFact("path was null"))
    }

    return check("readText()").that(actual!!.readText(charset))
  }

  @JvmOverloads
  public fun lines(charset: Charset = Charsets.UTF_8): IterableSubject {
    if (actual == null) {
      failWithActual(Fact.simpleFact("path was null"))
    }

    return check("readLines()").that(actual!!.readLines(charset))
  }
}
