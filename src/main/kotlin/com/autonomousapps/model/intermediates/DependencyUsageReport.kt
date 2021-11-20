package com.autonomousapps.model.intermediates

import com.autonomousapps.model.Coordinates

internal data class DependencyUsageReport(
  val buildType: String?,
  val flavor: String?,
  val variant: String,
  val annotationProcessorDependencies: Set<Trace>,
  val abiDependencies: Set<Trace>,
  val implDependencies: Set<Trace>,
  val compileOnlyDependencies: Set<Trace>,
  val runtimeOnlyDependencies: Set<Trace>,
  val compileOnlyApiDependencies: Set<Trace>,
  val unusedDependencies: Set<Trace>,
) {

  data class Trace(
    val coordinates: Coordinates,
    /** Any given dependency might be associated with 1+ reasons. */
    val reasons: Set<Reason>
  ) : Comparable<Trace> {
    override fun compareTo(other: Trace): Int = coordinates.compareTo(other.coordinates)
  }
}
