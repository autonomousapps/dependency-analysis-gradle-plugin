// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.declaration.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.declaration.Variant
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
  val version: String? = null,
  val configurationName: String,
  val gradleVariantIdentification: GradleVariantIdentification,
) : Comparable<Declaration> {

  override fun compareTo(other: Declaration): Int {
    return compareBy(Declaration::identifier)
      .thenComparing(compareBy<Declaration, String?>(nullsFirst()) { it.version })
      .thenComparing(compareBy(Declaration::configurationName))
      .thenComparing(compareBy(Declaration::gradleVariantIdentification))
      .compare(this, other)
  }

  val bucket: Bucket by unsafeLazy { Bucket.of(configurationName) }

  fun gav(): String = if (version != null) "$identifier:$version" else identifier

  fun variant(supportedSourceSets: Set<String>, hasCustomSourceSets: Boolean): Variant? {
    return Variant.of(configurationName, supportedSourceSets, hasCustomSourceSets)
  }
}
