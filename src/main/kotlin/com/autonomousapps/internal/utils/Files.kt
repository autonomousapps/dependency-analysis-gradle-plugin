package com.autonomousapps.internal.utils

import java.io.File

internal object Files {
  fun relativize(file: File, after: String): String {
    return file.absolutePath.substringAfter(after)
  }

  fun asPackagePath(file: File): String {
    return relativize(file, "build/classes/kotlin/main/")
  }
}
