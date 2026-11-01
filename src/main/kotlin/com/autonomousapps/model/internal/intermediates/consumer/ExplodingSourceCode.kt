// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.internal.CodeSource
import com.squareup.moshi.JsonClass

/** A single source file (e.g., `.java`, `.kt`) in this project. */
@JsonClass(generateAdapter = false)
internal data class ExplodingSourceCode(
  val relativePath: String,
  val className: String,
  val kind: CodeSource.Kind,
  val imports: Set<String>,
  /** The name of the source set for KMP projects, or null for non-KMP. */
  val kmpSourceSetName: String?,
) : Comparable<ExplodingSourceCode> {

  override fun compareTo(other: ExplodingSourceCode): Int {
    return compareBy<ExplodingSourceCode>(ExplodingSourceCode::relativePath)
      .thenBy(ExplodingSourceCode::className)
      .thenBy(ExplodingSourceCode::kind)
      .thenBy(LexicographicIterableComparator()) { it.imports }
      .thenComparing(compareBy<ExplodingSourceCode, String?>(nullsFirst()) { it.kmpSourceSetName })
      .compare(this, other)
  }
}
