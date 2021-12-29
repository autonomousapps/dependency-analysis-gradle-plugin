package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.configuration.Configurations
import com.autonomousapps.internal.unsafeLazy

/**
 * A dependency's declaration is the configuration that it's declared on (by user or plugin). A dependency may actually
 * be declared on more than one configuration, and that would not be an error.
 *
 * Declarations must be associated with a known "bucket" ([Bucket]) such as **implementation**, **api**, etc. That is,
 * analysis on ad hoc configurations is unsupported.
 */
internal data class Declaration(
  val identifier: String,
  val configurationName: String,
  val attributes: Set<Attribute> = emptySet()
) {

  val bucket: Bucket by unsafeLazy { Bucket.of(configurationName) }
  val variant: Variant by unsafeLazy { Configurations.variantFrom(configurationName) }
}

internal enum class Attribute {
  JAVA_PLATFORM,
  TEST_FIXTURE,
}
