package com.autonomousapps.model.declaration

import com.autonomousapps.internal.unsafeLazy
import com.squareup.moshi.JsonClass

/**
 * A dependency's declaration is the configuration that it's declared on (by user or plugin). A dependency may actually
 * be declared on more than one configuration, and that would not be an error.
 *
 * Declarations must be associated with a well-known [bucket] such as **implementation**, **api**, etc. That is,
 * analysis on ad hoc configurations is unsupported.
 *
 * Declarations are also always associated with a [variant]. For JVM projects (Java, Kotlin, etc.), this is a fancy way
 * of referring to the _source set_ (main, test). For Android projects, it is the combination of source set and
 * _variant_ (e.g., debug, release, buildTypeFlavor).
 */
@JsonClass(generateAdapter = false)
internal data class Declaration(
  val identifier: String,
  val configurationName: String,
  val targetFeatureVariantName: String = ""
) {

  val bucket: Bucket by unsafeLazy { Bucket.of(configurationName) }
  fun variant(supportedSourceSets: Set<String>, hasCustomSourceSets: Boolean): Variant? =
    Variant.of(configurationName, supportedSourceSets, hasCustomSourceSets)
}
