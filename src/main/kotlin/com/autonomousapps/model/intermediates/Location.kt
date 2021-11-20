package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.configuration.Configurations
import com.autonomousapps.internal.unsafeLazy

/**
 * A dependency's "location" is the configuration that it's connected to. A dependency may actually be connected to more
 * than one configuration, and that would not be an error.
 *
 * Locations must be associated with a known "bucket" ([Bucket]) such as **implementation**, **api**, etc. That is,
 * analysis on ad hoc configurations is unsupported.
 */
internal data class Location(
  val identifier: String,
  val configurationName: String,
  val attributes: Set<Attribute> = emptySet()
) {

  val bucket by unsafeLazy { Bucket.of(configurationName) }
  val variant by unsafeLazy { Configurations.variantFrom(configurationName) }
}

internal enum class Attribute {
  JAVA_PLATFORM,
  TEST_FIXTURE,
}
