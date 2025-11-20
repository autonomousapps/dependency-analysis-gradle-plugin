// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.declaration

import com.autonomousapps.model.GradleVariantIdentification
import com.autonomousapps.model.source.SourceKind
import com.squareup.moshi.JsonClass

/**
 * A dependency's declaration is the configuration that it's declared on (by user or plugin). A dependency may actually
 * be declared on more than one configuration, and that would not be an error.
 *
 * Declarations must be associated with a well-known [bucket] such as **implementation**, **api**, etc. That is,
 * analysis on ad hoc configurations is unsupported.
 *
 * Declarations are also always associated with a [sourceSetKind]. For JVM projects (Java, Kotlin, etc.), this is a fancy way
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
    return compareBy(Declaration::configurationName)
      .thenComparing(compareBy(Declaration::identifier))
      .thenComparing(compareBy<Declaration, String?>(nullsFirst()) { it.version })
      .thenComparing(compareBy(Declaration::gradleVariantIdentification))
      .compare(this, other)
  }

  private lateinit var _bucket: Bucket

  fun bucket(configurationNames: ConfigurationNames): Bucket {
    if (!::_bucket.isInitialized) {
      _bucket = Bucket.of(configurationName, configurationNames)
    }
    return _bucket
  }

  fun gav(): String = if (version != null) "$identifier:$version" else identifier

  fun sourceSetKind(
    hasCustomSourceSets: Boolean,
    configurationNames: ConfigurationNames,
  ): SourceKind? {
    return configurationNames.sourceKindFrom(
      configurationName = configurationName,
      hasCustomSourceSets = hasCustomSourceSets,
    )
  }
}
