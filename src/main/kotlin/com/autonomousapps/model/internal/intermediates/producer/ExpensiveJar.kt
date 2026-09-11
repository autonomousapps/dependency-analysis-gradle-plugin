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

  /**
   * A cache hit reuses the file-content-derived analysis, but the cached ExpensiveJar also carries the coordinates of
   * whichever artifact first populated this path in the build-scoped cache. Rebind to THIS artifact's identity;
   * otherwise a file shared by two dependencies (e.g. a classifier variant resolved by multiple projects) leaks the
   * other's coordinates and produces wrong advice. Note that Gradle does not provide the classifier in any public API,
   * so `Coordinates` does not (cannot?) model it.
   *
   * tl;dr: two Coordinates, one physical artifact.
   */
  fun withCoordinates(other: Coordinates): ExpensiveJar = copy(
    coordinates = other,
    explodedJar = explodedJar.copy(coordinates = other)
  )

  override fun compareTo(other: ExpensiveJar): Int {
    return compareBy<ExpensiveJar>(ExpensiveJar::coordinates)
      .thenBy(ExpensiveJar::explodedJar)
      .thenBy(LexicographicIterableComparator()) { it.binaryClasses }
      .compare(this, other)
  }
}
