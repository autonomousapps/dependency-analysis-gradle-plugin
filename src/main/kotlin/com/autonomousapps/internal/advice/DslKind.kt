// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import java.io.File

// TODO(tsr): this is exposed in tasks as an input. Move to non-internal package.
enum class DslKind {
  GROOVY,
  KOTLIN;

  companion object {
    fun from(file: File): DslKind {
      return when (file.name.substringAfter('.')) {
        "gradle" -> GROOVY
        "gradle.kts" -> KOTLIN
        else -> error("Unknown file type: $file")
      }
    }
  }
}
