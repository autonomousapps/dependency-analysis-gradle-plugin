// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.advice

import java.io.File

// Public because used as a task input, but effectively internal.
public enum class DslKind {
  GROOVY,
  KOTLIN;

  internal companion object {
    fun from(file: File): DslKind {
      return when (file.name.substringAfter('.')) {
        "gradle" -> GROOVY
        "gradle.kts" -> KOTLIN
        else -> error("Unknown file type: $file")
      }
    }
  }
}
