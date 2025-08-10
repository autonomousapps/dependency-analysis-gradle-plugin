// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.artifacts

import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

/** Essentially a wrapper around [path], with the intention to provide an expanded API eventually. */
public class BuildArtifact(private val path: Path) {

  /** The [Path] represented by this build artifact. */
  public val asPath: Path get() = path

  /** The [File] represented by this build artifact. */
  public val asFile: File get() = path.toFile()

  public fun exists(): Boolean = path.exists()
  public fun notExists(): Boolean = path.notExists()
  public fun isRegularFile(): Boolean = path.isRegularFile()
  public fun isDirectory(): Boolean = path.isDirectory()
  public fun isSymbolicLink(): Boolean = path.isSymbolicLink()

  public val extension: String get() = path.extension
}

internal fun Path.toBuildArtifact(): BuildArtifact = BuildArtifact(this)
