// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import java.io.File

internal object Files {
  fun relativize(file: File, after: String): String {
    return file.absoluteFile.invariantSeparatorsPath.substringAfter(after)
  }

  fun asPackagePath(file: File): String {
    return relativize(file, "build/classes/kotlin/main/")
  }
}
