// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.*
import com.autonomousapps.model.internal.intermediates.DependencyView
import com.autonomousapps.model.internal.intermediates.ExplodingJar
import com.squareup.moshi.JsonClass
import java.io.File

/**
 * A library or project, along with the set of classes declared by, and other information contained within, this
 * exploded jar. This is the serialized form of [ExplodingJar].
 */
@JsonClass(generateAdapter = false)
internal data class ExplodedJar(

  override val coordinates: Coordinates,
  val jarFile: File,

  /**
   * True if this dependency contains only annotation that are only needed at compile-time (`CLASS`
   * and `SOURCE` level retention policies). False otherwise.
   */
  val isCompileOnlyAnnotations: Boolean = false,
  /**
   * The set of classes that are service providers (they extend [java.security.Provider]). May be
   * empty.
   */
  val securityProviders: Set<String> = emptySet(),
  /**
   * Android Lint registry, if there is one. May be null.
   */
  val androidLintRegistry: String? = null,
  /**
   * True if this component contains _only_ an Android Lint jar/registry. If this is true,
   * [androidLintRegistry] must be non-null.
   */
  val isLintJar: Boolean = false,
  /**
   * The classes (with binary member signatures) provided by this library.
   */
  val binaryClasses: Set<BinaryClass>,
  /**
   * A map of each class declared by this library to the set of constants it defines. The latter may
   * be empty for any given declared class.
   */
  val constantFields: Map<String, Set<String>>,
  /**
   * All of the "Kt" files within this component.
   */
  val ktFiles: Set<KtFile>,
) : DependencyView<ExplodedJar> {

  internal constructor(
    artifact: PhysicalArtifact,
    exploding: ExplodingJar,
  ) : this(
    coordinates = artifact.coordinates,
    jarFile = artifact.file,
    isCompileOnlyAnnotations = exploding.isCompileOnlyCandidate,
    securityProviders = exploding.securityProviders,
    androidLintRegistry = exploding.androidLintRegistry,
    isLintJar = exploding.isLintJar,
    binaryClasses = exploding.binaryClasses,
    constantFields = exploding.constants,
    ktFiles = exploding.ktFiles
  )

  override fun compareTo(other: ExplodedJar): Int {
    return coordinates.compareTo(other.coordinates).let {
      if (it == 0) jarFile.compareTo(other.jarFile) else it
    }
  }

  init {
    if (isLintJar && androidLintRegistry == null) {
      throw IllegalStateException("Android lint jar for $coordinates must contain a lint registry")
    }
  }

  override fun toCapabilities(): List<Capability> {
    val capabilities = mutableListOf<Capability>()
    capabilities += InferredCapability(isCompileOnlyAnnotations)
    binaryClasses.ifNotEmpty { capabilities += BinaryClassCapability.newInstance(it) }
    constantFields.ifNotEmpty { capabilities += ConstantCapability.newInstance(it, ktFiles) }
    securityProviders.ifNotEmpty { capabilities += SecurityProviderCapability.newInstance(it) }
    androidLintRegistry?.let { capabilities += AndroidLinterCapability(it, isLintJar) }
    return capabilities
  }
}
