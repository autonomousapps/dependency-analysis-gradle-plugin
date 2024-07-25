// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.intermediates.Usage

internal fun Map<Coordinates, Set<Usage>>.toStringCoordinates(buildPath: String): Map<String, Set<Usage>> {
  val result = mutableMapOf<String, MutableSet<Usage>>()
  forEach { (coordinates, usages) ->
    result.computeIfAbsent(toCoordinatesKey(coordinates, buildPath)) { mutableSetOf() }.addAll(usages)
  }
  return result
}

internal fun toCoordinatesKey(coordinates: Coordinates, buildPath: String) =
  coordinates.gav() + when {
    // If the coordinates point at a project in the current build, there are two notations to address it:
    // 1) 'group:name' coordinates, which is the main representation (see IncludedBuildCoordinates)
    // 2) The project path notation starting with a ':' (:project-name)
    // To allow notation 2) to address a project in the 'reasons' task, we add this coordinate to the key as well if applicable (separated by '|')
    coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath -> "|${coordinates.resolvedProject.gav()}"
    else -> ""
  }

internal fun <T> String.matchesKey(mapEntry: Map.Entry<String, T>): Boolean {
  // first check for an exact match (if user passes in full GAV).
  if (equalsKey(mapEntry)) return true

  // if user passes in GA (no V), append ':' (e.g., avoid returning okio-jvm when user passed in okio).
  if ("${this}:".startsWithKey(mapEntry)) return true

  // finally the most lenient check
  if (startsWithKey(mapEntry)) return true

  return false
}

internal fun <T> String.equalsKey(mapEntry: Map.Entry<String, T>): Boolean {
  val firstSegment = mapEntry.key.firstCoordinatesKeySegment()
  val tokens = firstSegment.split(":")

  // module coordinates, "group:artifact:version"
  if (tokens.size == 3 && this == firstSegment) {
    return true
  }

  return firstSegment == this || mapEntry.key.secondCoordinatesKeySegment() == this
}

private fun <T> String.startsWithKey(mapEntry: Map.Entry<String, T>) =
  mapEntry.key.firstCoordinatesKeySegment().startsWith(this)
    || mapEntry.key.secondCoordinatesKeySegment()?.startsWith(this) == true

/** First key segment is always 'group:name' coordinates */
internal fun String.firstCoordinatesKeySegment(): String =
  if (contains("|")) split("|")[0] else this

/** Key might contain a second segment for alternative project path notation ':project-name' */
internal fun String.secondCoordinatesKeySegment(): String? =
  if (contains("|")) split("|")[1] else null
