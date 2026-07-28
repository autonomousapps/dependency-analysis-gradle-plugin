// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.model.Coordinates
import com.squareup.moshi.JsonClass

/** So-called because [BinaryClass]es can be *quite* large. */
@JsonClass(generateAdapter = false)
internal data class ExpensiveJar(
  val coordinates: Coordinates,
  val explodedJar: ExplodedJar,
  val binaryClasses: Set<BinaryClass>,
) : Comparable<ExpensiveJar> {
  override fun compareTo(other: ExpensiveJar): Int {
    return compareBy<ExpensiveJar>(ExpensiveJar::coordinates)
      .thenBy(ExpensiveJar::explodedJar)
      .thenBy(LexicographicIterableComparator()) { it.binaryClasses }
      .compare(this, other)
  }
}
