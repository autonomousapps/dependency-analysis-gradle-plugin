// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass

/** Only public because it's a task input. */
@JsonClass(generateAdapter = false)
public data class SourceSetFiles(
  /** e.g., "commonMain", "commonTest". */
  val sourceSetName: String,
  /** e.g., "src/commonMain/kotlin", "build/gen/kotlin". */
  val sourceBase: String,
  /** e.g., "com/foo/Foo.kt", "generated.kt". */
  val sourceFiles: Set<String>,
) : Comparable<SourceSetFiles> {

  init {
    assert(sourceFiles.isNotEmpty()) { "Expected non-empty 'sourceFiles'." }
    assert(sourceBase.isNotBlank()) { "Expected non-blank 'sourceBase'." }
  }

  override fun compareTo(other: SourceSetFiles): Int {
    return compareBy(SourceSetFiles::sourceSetName)
      .thenBy(SourceSetFiles::sourceBase)
      .thenBy(LexicographicIterableComparator()) { it.sourceFiles }
      .compare(this, other)
  }
}
