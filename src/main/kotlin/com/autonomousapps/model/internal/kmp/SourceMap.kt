// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.kmp

import com.autonomousapps.internal.utils.LexicographicIterableComparator

internal data class SourceMap(
  /** e.g., "commonMain", "commonTest". */
  val sourceSetName: String,
  /** e.g., "src/commonMain/kotlin", "build/gen/kotlin". */
  val sourceBase: String,
  /** e.g., "com/foo/Foo.kt", "generated.kt". */
  val sourceFiles: Set<String>,
) : Comparable<SourceMap> {
  override fun compareTo(other: SourceMap): Int {
    return compareBy(SourceMap::sourceSetName)
      .thenBy(SourceMap::sourceBase)
      .thenBy(LexicographicIterableComparator()) { it.sourceFiles }
      .compare(this, other)
  }
}
