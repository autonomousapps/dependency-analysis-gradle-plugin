package com.autonomousapps.internal.utils

import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.IncludedBuildCoordinates
import com.autonomousapps.model.intermediates.Usage

internal fun Map<Coordinates, Set<Usage>>.toStringCoordinates(buildPath: String): Map<String, Set<Usage>> =
  map { (key, value) ->
    toCoordinatesKey(key, buildPath) to value
  }.toMap()

internal fun toCoordinatesKey(coordinates: Coordinates, buildPath: String) =
  coordinates.gav() + when {
    // If the coordinates point at a project in the current build, there are two notations to address it:
    // 1) 'group:name' coordinates, which is the main representation (see IncludedBuildCoordinates)
    // 2) The project path notation starting with a ':' (:project-name)
    // To allow notation 2) to address a project in the 'reasons' task, we add this coordinate to the key as well if applicable (separated by '|')
    coordinates is IncludedBuildCoordinates && coordinates.resolvedProject.buildPath == buildPath -> "|${coordinates.resolvedProject.gav()}"
    else -> ""
  }

internal fun <T> String.equalsKey(mapEntry: Map.Entry<String, T>) =
  mapEntry.firstKeySegment() == this || mapEntry.secondKeySegment() == this

internal fun <T> String.startsWithKey(mapEntry: Map.Entry<String, T>) =
  mapEntry.firstKeySegment().startsWith(this) || mapEntry.secondKeySegment()?.startsWith(this) == true

/** First key segment is always 'group:name' coordinates */
internal fun <T> Map.Entry<String, T>.firstKeySegment(): String =
  if (key.contains("|")) key.split("|")[0] else key

/** Key might contain a second segment for alternative project path notation ':project-name' */
internal fun <T> Map.Entry<String, T>.secondKeySegment(): String? =
  if (key.contains("|")) key.split("|")[1] else null
