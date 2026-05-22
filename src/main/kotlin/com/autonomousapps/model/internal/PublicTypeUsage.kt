// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass

/**
 * A global report of public type usage. A public type that has no external accessors could have its visibility reduced.
 *
 * TODO(tsr): this is a candidate to be public
 *
 * @see [PublicTypes]
 */
@JsonClass(generateAdapter = false)
internal data class PublicTypeUsage(
  val reports: Set<Report>,
) {

  @JsonClass(generateAdapter = false)
  data class Report(
    /** The project that owns the public types being accessed (see [accesses]). */
    val owningProject: String,

    /** Each accessed public type, along with the accessing projects. May be empty. */
    val accesses: Set<Accesses>,

    /** Public types that are not accessed outside. May be empty. */
    val unaccessedTypes: Set<String>,
  ) : Comparable<Report> {
    override fun compareTo(other: Report): Int {
      return compareBy(Report::owningProject)
        .thenBy(LexicographicIterableComparator()) { it.accesses }
        .thenBy(LexicographicIterableComparator()) { it.unaccessedTypes }
        .compare(this, other)
    }
  }

  /** Contains all [accessors][accessingProjects] of [typeName]. [accessingProjects] cannot be empty. */
  @JsonClass(generateAdapter = false)
  data class Accesses(
    /** The fully-qualified type being accessed. */
    val typeName: String,
    /** Guaranteed to be non-empty. */
    val accessingProjects: Set<String>,
  ) : Comparable<Accesses> {

    init {
      require(accessingProjects.isNotEmpty()) {
        "Accessing projects must not be empty. Was empty for '$typeName'."
      }
    }

    override fun compareTo(other: Accesses): Int {
      return compareBy(Accesses::typeName)
        .thenBy(LexicographicIterableComparator()) { it.accessingProjects }
        .compare(this, other)
    }
  }
}
