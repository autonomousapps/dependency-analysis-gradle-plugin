// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass

/**
 * The set of all public types provided by this project.
 *
 * TODO(tsr): consider making this public.
 *
 * @see [PublicTypeUsage]
 */
@JsonClass(generateAdapter = false)
internal data class PublicTypes(
  /** The path of the owning project, e.g., `":feature:home"`. */
  val projectPath: String,

  /** Types that have public visibility. */
  val types: Set<String>,
) : Comparable<PublicTypes> {

  override fun compareTo(other: PublicTypes): Int {
    return compareBy(PublicTypes::projectPath)
      .thenBy(LexicographicIterableComparator()) { it.types }
      .compare(this, other)
  }
}
