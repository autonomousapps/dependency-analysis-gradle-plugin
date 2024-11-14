// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class ExplodingBytecode(
  val relativePath: String,

  /** The name of this class. */
  val className: String,

  /** The path to the source file for this class. TODO: how does this differ from [relativePath]? */
  val sourceFile: String?,

  /** Every class discovered in the bytecode of [className], and not as an annotation. */
  val nonAnnotationClasses: Set<String>,

  /** Every class discovered in the bytecode of [className], and as a visible annotation. */
  val annotationClasses: Set<String>,

  /** Every class discovered in the bytecode of [className], and as an invisible annotation. */
  val invisibleAnnotationClasses: Set<String>,

  /** Every [MemberAccess] to another class from [this class][className]. */
  val binaryClassAccesses: Map<String, Set<MemberAccess>>,
)
