// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.artifacts

import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isSymbolicLink

public enum class FileType(public val humanReadableName: String) {
  REGULAR_FILE("regular file"),
  DIRECTORY("directory"),
  SYMLINK("symbolic link"),
  UNKNOWN("unknown");

  public companion object {
    public fun from(buildArtifact: BuildArtifact, vararg options: LinkOption): FileType {
      return from(buildArtifact.asPath, *options)
    }

    public fun from(path: Path, vararg options: LinkOption): FileType = when {
      path.isRegularFile(*options) -> REGULAR_FILE
      path.isDirectory(*options) -> DIRECTORY
      path.isSymbolicLink() -> SYMLINK
      else -> UNKNOWN
    }
  }
}
