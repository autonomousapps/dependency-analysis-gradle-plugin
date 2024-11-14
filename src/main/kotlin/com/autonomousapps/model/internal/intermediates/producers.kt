// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates

import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.*
import com.autonomousapps.model.internal.AndroidAssetCapability
import com.autonomousapps.model.internal.AndroidManifestCapability
import com.autonomousapps.model.internal.AndroidResCapability
import com.autonomousapps.model.internal.AnnotationProcessorCapability
import com.autonomousapps.model.internal.Capability
import com.autonomousapps.model.internal.InlineMemberCapability
import com.autonomousapps.model.internal.NativeLibCapability
import com.autonomousapps.model.internal.ServiceLoaderCapability
import com.autonomousapps.model.internal.TypealiasCapability
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.result.ResolvedArtifactResult

internal interface DependencyView<T> : Comparable<T> where T : DependencyView<T> {
  val coordinates: Coordinates
  fun toCapabilities(): List<Capability>

  override fun compareTo(other: T): Int = coordinates.compareTo(other.coordinates)
}

/**
 * A dependency that includes a lint jar. (Which is maybe always named lint.jar?)
 *
 * Example registry: `nl.littlerobots.rxlint.RxIssueRegistry`.
 *
 * nb: Deliberately does not implement [DependencyView]. For various reasons, this information gets embedded in
 * [ExplodedJar][com.autonomousapps.model.intermediates.producer.ExplodedJar], which is the preferred access point for
 * deeper analysis.
 */
@JsonClass(generateAdapter = false)
internal data class AndroidLinterDependency(
  val coordinates: Coordinates,
  val lintRegistry: String,
) : Comparable<AndroidLinterDependency> {
  override fun compareTo(other: AndroidLinterDependency): Int = coordinates.compareTo(other.coordinates)
}

/** Metadata from an Android manifest. */
@JsonClass(generateAdapter = false)
internal data class AndroidManifestDependency(
  override val coordinates: Coordinates,
  /** A map of component type to components. */
  val componentMap: Map<AndroidManifestCapability.Component, Set<String>>,
) : DependencyView<AndroidManifestDependency> {

  constructor(
    componentMap: Map<AndroidManifestCapability.Component, Set<String>>,
    artifact: ResolvedArtifactResult,
  ) : this(
    componentMap = componentMap,
    coordinates = artifact.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(AndroidManifestCapability(componentMap))
}

/** A dependency that includes Android assets (e.g., src/main/assets). A runtime dependency. */
@JsonClass(generateAdapter = false)
internal data class AndroidAssetDependency(
  override val coordinates: Coordinates,
  val assets: List<String>,
) : DependencyView<AndroidAssetDependency> {

  override fun toCapabilities(): List<Capability> = listOf(AndroidAssetCapability(assets))
}

@JsonClass(generateAdapter = false)
internal data class AndroidResDependency(
  override val coordinates: Coordinates,
  /** An import that indicates a possible use of an Android resource from this dependency. */
  val import: String,
  val lines: List<AndroidResCapability.Line>,
) : DependencyView<AndroidResDependency> {

  override fun toCapabilities(): List<Capability> = listOf(AndroidResCapability(import, lines))
}

@JsonClass(generateAdapter = false)
internal data class AnnotationProcessorDependency(
  override val coordinates: Coordinates,
  val processor: String,
  val supportedAnnotationTypes: Set<String>,
) : DependencyView<AnnotationProcessorDependency> {

  constructor(
    processor: String,
    supportedAnnotationTypes: Set<String>,
    artifact: ResolvedArtifactResult,
  ) : this(
    processor = processor,
    supportedAnnotationTypes = supportedAnnotationTypes,
    coordinates = artifact.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(
    AnnotationProcessorCapability(processor, supportedAnnotationTypes)
  )
}

@JsonClass(generateAdapter = false)
internal data class InlineMemberDependency(
  override val coordinates: Coordinates,
  val inlineMembers: Set<InlineMemberCapability.InlineMember>,
) : DependencyView<InlineMemberDependency> {

  override fun toCapabilities(): List<Capability> = listOf(InlineMemberCapability(inlineMembers))
}

@JsonClass(generateAdapter = false)
internal data class TypealiasDependency(
  override val coordinates: Coordinates,
  val typealiases: Set<TypealiasCapability.Typealias>,
) : DependencyView<TypealiasDependency> {

  override fun toCapabilities(): List<Capability> = listOf(TypealiasCapability(typealiases))
}

@JsonClass(generateAdapter = false)
internal data class NativeLibDependency(
  override val coordinates: Coordinates,
  val fileNames: Set<String>,
) : DependencyView<NativeLibDependency> {

  override fun toCapabilities(): List<Capability> = listOf(NativeLibCapability(fileNames))
}

@JsonClass(generateAdapter = false)
internal data class ServiceLoaderDependency(
  override val coordinates: Coordinates,
  val providerFile: String,
  val providerClasses: Set<String>,
) : DependencyView<ServiceLoaderDependency> {

  constructor(
    providerFile: String,
    providerClasses: Set<String>,
    artifact: ResolvedArtifactResult,
  ) : this(
    providerFile = providerFile,
    providerClasses = providerClasses,
    coordinates = artifact.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(ServiceLoaderCapability(providerFile, providerClasses))
}
