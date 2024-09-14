// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.intermediates

import com.autonomousapps.model.CodeSource
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

@JsonClass(generateAdapter = false)
internal data class ExplodingBytecode(
  val relativePath: String,

  /** The name of this class. */
  val className: String,

  /** The path to the source file for this class. TODO: how does this differ from [relativePath]? */
  val sourceFile: String?,

  /** Every class discovered in the bytecode of [className], and not as an annotation. */
  val usedNonAnnotationClasses: Set<String>,

  /** Every class discovered in the bytecode of [className], and as an annotation. */
  val usedAnnotationClasses: Set<String>,
)

@JsonClass(generateAdapter = false)
internal data class ExplodingAbi(
  val className: String,
  val sourceFile: String?,
  /** Every class discovered in the bytecode of [className], and which is exposed as part of the ABI. */
  val exposedClasses: Set<String>
) : Comparable<ExplodingAbi> {
  override fun compareTo(other: ExplodingAbi): Int = className.compareTo(other.className)
}
