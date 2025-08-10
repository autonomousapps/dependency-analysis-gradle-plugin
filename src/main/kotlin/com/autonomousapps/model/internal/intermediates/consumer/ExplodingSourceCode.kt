// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.autonomousapps.model.internal.CodeSource
import com.squareup.moshi.JsonClass

/** A single source file (e.g., `.java`, `.kt`) in this project. */
@JsonClass(generateAdapter = false)
internal data class ExplodingSourceCode(
  val relativePath: String,
  val className: String,
  val kind: CodeSource.Kind,
  val imports: Set<String>
) : Comparable<ExplodingSourceCode> {

  override fun compareTo(other: ExplodingSourceCode): Int = relativePath.compareTo(other.relativePath)
}
