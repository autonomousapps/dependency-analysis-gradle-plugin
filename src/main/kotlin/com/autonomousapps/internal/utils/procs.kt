package com.autonomousapps.internal.utils

import com.autonomousapps.internal.AnnotationProcessor

/**
 * Because there is a 1-to-many relationship between dependencies and annotation processors (one dependency can
 * declare many annotation processors; see AutoValue for example), our set difference needs to consider _only_
 * the dependency itself.
 */
internal operator fun Set<AnnotationProcessor>.minus(other: Set<AnnotationProcessor>): Set<AnnotationProcessor> {
  // Initialize with full set
  val difference = mutableSetOf<AnnotationProcessor>().apply { addAll(this@minus) }
  // Now remove from set all matches in `other`
  for (proc in this) {
    if (other.any { it.dependency == proc.dependency }) {
      difference.remove(proc)
    }
  }
  return difference
}
