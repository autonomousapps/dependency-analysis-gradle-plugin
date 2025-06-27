// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.internal.intermediates.Usage

/** A stringified version of [Coordinates], used when we need to de/serialize a Coordinates as a map key. */
internal class CoordinatesString(
  /** The coordinates including the build path, if this is an [IncludedBuildCoordinates]. */
  val fullBuildGav: String,
  /** The coordinates to the local project, if this is a local project. Otherwise null. */
  val localGav: String?,
  /** The set of capabilities for this dependency, e.g. `"test-fixtures[,other]*"`. */
  val capabilities: Set<String>?,
) {

  init {
    if (localGav != null) {
      require(localGav.startsWith(":")) { "localGav must start with ':'. Was '$localGav'." }
    }
  }

  fun fullGav(): String = buildString {
    append(fullBuildGav)

    if (localGav != null) {
      append(SEPARATOR)
      append(localGav)
    }
  }

  override fun toString(): String = buildString {
    append(fullBuildGav)

    if (localGav != null) {
      append(SEPARATOR)
      append(localGav)
    }

    if (capabilities != null) {
      append(SEPARATOR)
      append(capabilities.joinToString(separator = CAPABILITIES_SEPARATOR))
    }
  }

  fun matches(coordinates: Coordinates): Boolean {
    val byGav = matchesGav(coordinates)
    val byCapabilities = matchesCapabilities(coordinates)

    return byGav && byCapabilities
  }

  private fun matchesGav(coordinates: Coordinates): Boolean {
    val requestedId = coordinates.gav()
    val tokens = fullBuildGav.split(":")

    // module coordinates, "group:artifact:version"
    if (tokens.size == 3 && requestedId == fullBuildGav) {
      return true
    }

    return fullBuildGav == requestedId || localGav == requestedId
  }

  private fun matchesCapabilities(coordinates: Coordinates): Boolean {
    val requestedCapabilities = coordinates.gradleVariantIdentification.capabilities

    if (requestedCapabilities.isEmpty() && capabilities == null) return true
    if (requestedCapabilities.size != capabilities?.size) return false

    return requestedCapabilities == capabilities
  }

  internal companion object {
    const val SEPARATOR = "|"
    const val CAPABILITIES_SEPARATOR = ","

    fun of(string: String): CoordinatesString {
      val parts = string.split(SEPARATOR)
      require(parts.size <= 3) {
        "Coordinates string must have a maximum of three parts. Was '$string'."
      }

      val fullBuildGav = parts[0]
      var localGav: String? = null
      var capabilities: Set<String>? = null

      if (parts.size == 2) {
        if (parts[1].startsWith(":")) {
          localGav = parts[1]
        } else {
          capabilities = parts[1].toCapabilities()
        }
      } else if (parts.size == 3) {
        localGav = parts[1]
        capabilities = parts[2].toCapabilities()
      }

      return CoordinatesString(
        fullBuildGav = fullBuildGav,
        localGav = localGav,
        capabilities = capabilities,
      )
    }

    private fun String.toCapabilities(): Set<String> = split(CAPABILITIES_SEPARATOR).toSet()

    fun toStringCoordinates(
      map: Map<Coordinates, Set<Usage>>,
      buildPath: String
    ): Map<String, Set<Usage>> {
      val result = mutableMapOf<String, MutableSet<Usage>>()
      map.forEach { (coordinates, usages) ->
        result.computeIfAbsent(toCoordinatesKey(coordinates, buildPath)) { mutableSetOf() }.addAll(usages)
      }
      return result
    }

    private fun toCoordinatesKey(coordinates: Coordinates, buildPath: String): String {
      val fullBuildGav = coordinates.gav()
      val localGav = when {
        // If the coordinates point at a project in the current build, there are two notations to address it:
        // 1) 'group:name' coordinates, which is the main representation (see IncludedBuildCoordinates)
        // 2) The project path notation starting with a ':' (:project-name)
        // To allow notation 2) to address a project in the 'reasons' task, we add this coordinate to the key as well if applicable (separated by '|')
        coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath -> {
          coordinates.resolvedProject.gav()
        }

        else -> null
      }
      val capabilities = coordinates.gradleVariantIdentification.capabilities.ifEmpty { null }

      val coordinatesString = CoordinatesString(
        fullBuildGav = fullBuildGav,
        localGav = localGav,
        capabilities = capabilities,
      )

      return coordinatesString.toString()
    }

    fun <T> matchesKey(requestedId: String, mapEntry: Map.Entry<String, T>): Boolean {
      // first check for an exact match (if user passes in full GAV).
      if (equalsKey(requestedId, mapEntry)) return true

      // if user passes in GA (no V), append ':' (e.g., avoid returning okio-jvm when user passed in okio).
      if (startsWithKey("${requestedId}:", mapEntry)) return true

      // finally the most lenient check
      if (startsWithKey(requestedId, mapEntry)) return true

      return false
    }

    fun <T> equalsKey(requestedId: String, mapEntry: Map.Entry<String, T>): Boolean {
      return equalsKey(requestedId, mapEntry.key)
    }

    fun equalsKey(requestedId: String, key: String): Boolean {
      val firstSegment = firstCoordinatesKeySegment(key)
      val tokens = firstSegment.split(":")

      // module coordinates, "group:artifact:version"
      if (tokens.size == 3 && requestedId == firstSegment) {
        return true
      }

      return firstSegment == requestedId || secondCoordinatesKeySegment(key) == requestedId
    }

    private fun <T> startsWithKey(requestedId: String, mapEntry: Map.Entry<String, T>): Boolean {
      return startsWithKey(requestedId, mapEntry.key)
    }

    private fun startsWithKey(requestedId: String, key: String): Boolean {
      return firstCoordinatesKeySegment(key).startsWith(requestedId)
        || secondCoordinatesKeySegment(key)?.startsWith(requestedId) == true
    }

    /** First key segment is always 'group:name' coordinates. */
    fun firstCoordinatesKeySegment(s: String): String {
      return of(s).fullBuildGav
    }

    /** Key might contain a second segment for alternative project path notation ':project-name'. */
    fun secondCoordinatesKeySegment(s: String): String? {
      return of(s).localGav
    }

    /** Key might contain a third segment indicating dependency capabilities. */
    fun thirdCoordinatesKeySegment(s: String): Set<String>? {
      return of(s).capabilities
    }
  }
}
