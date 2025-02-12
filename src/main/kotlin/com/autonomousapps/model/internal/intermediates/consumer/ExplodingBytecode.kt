// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.consumer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.MapSetComparator
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class ExplodingBytecode(
  val relativePath: String,

  /** The name of this class. */
  val className: String,

  /** The super class of this class. May be null (for `java/lang/Object`). */
  val superClass: String?,

  /** The interfaces of this class (may be empty). */
  val interfaces: Set<String>,

  /** The path to the source file for this class. TODO(tsr): how does this differ from [relativePath]? */
  val sourceFile: String?,

  /** Every class discovered in the bytecode of [className], and not as an annotation. */
  val nonAnnotationClasses: Set<String>,

  /** Every class discovered in the bytecode of [className], and as a visible annotation. */
  val annotationClasses: Set<String>,

  /** Every class discovered in the bytecode of [className], and as an invisible annotation. */
  val invisibleAnnotationClasses: Set<String>,

  /** Every [MemberAccess] to another class from [this class][className]. */
  val binaryClassAccesses: Map<String, Set<MemberAccess>>,
) : Comparable<ExplodingBytecode> {

  override fun compareTo(other: ExplodingBytecode): Int {
    return compareBy(ExplodingBytecode::relativePath)
      .thenComparing(ExplodingBytecode::className)
      .thenComparing(compareBy<ExplodingBytecode, String?>(nullsFirst()) { it.superClass })
      .thenBy(LexicographicIterableComparator()) { it.interfaces }
      .thenComparing(compareBy<ExplodingBytecode, String?>(nullsFirst()) { it.sourceFile })
      .thenBy(LexicographicIterableComparator()) { it.nonAnnotationClasses }
      .thenBy(LexicographicIterableComparator()) { it.annotationClasses }
      .thenBy(LexicographicIterableComparator()) { it.invisibleAnnotationClasses }
      .thenBy(MapSetComparator()) { it.binaryClassAccesses }
      .compare(this, other)
  }
}
