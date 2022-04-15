package com.autonomousapps.internal.advice

import java.io.File

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
