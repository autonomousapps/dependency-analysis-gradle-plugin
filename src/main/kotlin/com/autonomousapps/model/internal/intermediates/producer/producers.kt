// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal.intermediates.producer

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.MapSetComparator
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.Coordinates
import com.autonomousapps.model.internal.*
import com.squareup.moshi.JsonClass
import org.gradle.api.artifacts.result.ResolvedArtifactResult

internal interface DependencyView<T> : Comparable<T> where T : DependencyView<T> {
  val coordinates: Coordinates
  fun toCapabilities(): List<Capability>
}

/**
 * A dependency that includes a lint jar. (Which is maybe always named lint.jar?)
 *
 * Example registry: `nl.littlerobots.rxlint.RxIssueRegistry`.
 *
 * nb: Deliberately does not implement [DependencyView]. For various reasons, this information gets embedded in
 * [ExplodedJar], which is the preferred access
 * point for deeper analysis.
 */
@JsonClass(generateAdapter = false)
internal data class AndroidLinterDependency(
  val coordinates: Coordinates,
  val lintRegistry: String,
) : Comparable<AndroidLinterDependency> {
  override fun compareTo(other: AndroidLinterDependency): Int {
    return compareBy(AndroidLinterDependency::coordinates)
      .thenComparing(AndroidLinterDependency::lintRegistry)
      .compare(this, other)
  }
}

/** Metadata from an Android manifest. */
@JsonClass(generateAdapter = false)
internal data class AndroidManifestDependency(
  override val coordinates: Coordinates,
  /** A map of component type to components. */
  val componentMap: Map<AndroidManifestCapability.Component, Set<String>>,
) : DependencyView<AndroidManifestDependency> {

  companion object {
    fun newInstance(
      componentMap: Map<AndroidManifestCapability.Component, Set<String>>,
      artifact: ResolvedArtifactResult,
    ): AndroidManifestDependency {
      return AndroidManifestDependency(artifact.toCoordinates(), componentMap.toSortedMap().efficient())
    }
  }

  override fun compareTo(other: AndroidManifestDependency): Int {
    return compareBy(AndroidManifestDependency::coordinates)
      .thenBy(MapSetComparator()) { it.componentMap }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(AndroidManifestCapability.newInstance(componentMap))
}

/** A dependency that includes Android assets (e.g., src/main/assets). A runtime dependency. */
@JsonClass(generateAdapter = false)
internal data class AndroidAssetDependency(
  override val coordinates: Coordinates,
  val assets: List<String>,
) : DependencyView<AndroidAssetDependency> {

  companion object {
    fun newInstance(coordinates: Coordinates, assets: List<String>): AndroidAssetDependency {
      return AndroidAssetDependency(coordinates, assets.sorted().efficient())
    }
  }

  override fun compareTo(other: AndroidAssetDependency): Int {
    return compareBy(AndroidAssetDependency::coordinates)
      .thenBy(LexicographicIterableComparator()) { it.assets }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(AndroidAssetCapability.newInstance(assets))
}

@JsonClass(generateAdapter = false)
internal data class AndroidResDependency(
  override val coordinates: Coordinates,
  /** An import that indicates a possible use of an Android resource from this dependency. */
  val import: String,
  val lines: List<AndroidResCapability.Line>,
) : DependencyView<AndroidResDependency> {

  companion object {
    fun newInstance(
      coordinates: Coordinates,
      import: String,
      lines: List<AndroidResCapability.Line>,
    ): AndroidResDependency {
      return AndroidResDependency(coordinates, import, lines.sorted().efficient())
    }
  }

  override fun compareTo(other: AndroidResDependency): Int {
    return compareBy(AndroidResDependency::coordinates)
      .thenComparing(compareBy(AndroidResDependency::import))
      .thenBy(LexicographicIterableComparator()) { it.lines }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(AndroidResCapability.newInstance(import, lines))
}

@JsonClass(generateAdapter = false)
internal data class AnnotationProcessorDependency(
  override val coordinates: Coordinates,
  val processor: String,
  val supportedAnnotationTypes: Set<String>,
) : DependencyView<AnnotationProcessorDependency> {

  companion object {
    fun newInstance(
      processor: String,
      supportedAnnotationTypes: Set<String>,
      artifact: ResolvedArtifactResult,
    ): AnnotationProcessorDependency {
      return AnnotationProcessorDependency(
        artifact.toCoordinates(),
        processor,
        supportedAnnotationTypes.toSortedSet().efficient()
      )
    }
  }

  override fun compareTo(other: AnnotationProcessorDependency): Int {
    return compareBy(AnnotationProcessorDependency::coordinates)
      .thenComparing(AnnotationProcessorDependency::processor)
      .thenBy(LexicographicIterableComparator()) { it.supportedAnnotationTypes }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(
    AnnotationProcessorCapability.newInstance(processor, supportedAnnotationTypes)
  )
}

@JsonClass(generateAdapter = false)
internal data class InlineMemberDependency(
  override val coordinates: Coordinates,
  val inlineMembers: Set<InlineMemberCapability.InlineMember>,
) : DependencyView<InlineMemberDependency> {

  companion object {
    fun newInstance(
      coordinates: Coordinates,
      inlineMembers: Set<InlineMemberCapability.InlineMember>,
    ): InlineMemberDependency {
      return InlineMemberDependency(coordinates, inlineMembers.toSortedSet().efficient())
    }
  }

  override fun compareTo(other: InlineMemberDependency): Int {
    return compareBy(InlineMemberDependency::coordinates)
      .thenBy(LexicographicIterableComparator()) { it.inlineMembers }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(InlineMemberCapability.newInstance(inlineMembers))
}

@JsonClass(generateAdapter = false)
internal data class TypealiasDependency(
  override val coordinates: Coordinates,
  val typealiases: Set<TypealiasCapability.Typealias>,
) : DependencyView<TypealiasDependency> {

  companion object {
    fun newInstance(coordinates: Coordinates, typealiases: Set<TypealiasCapability.Typealias>): TypealiasDependency {
      return TypealiasDependency(coordinates, typealiases.toSortedSet().efficient())
    }
  }

  override fun compareTo(other: TypealiasDependency): Int {
    return compareBy(TypealiasDependency::coordinates)
      .thenBy(LexicographicIterableComparator()) { it.typealiases }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(TypealiasCapability.newInstance(typealiases))
}

@JsonClass(generateAdapter = false)
internal data class NativeLibDependency(
  override val coordinates: Coordinates,
  val fileNames: Set<String>,
) : DependencyView<NativeLibDependency> {

  companion object {
    fun newInstance(coordinates: Coordinates, fileNames: Set<String>): NativeLibDependency {
      return NativeLibDependency(coordinates, fileNames.toSortedSet().efficient())
    }
  }

  override fun compareTo(other: NativeLibDependency): Int {
    return compareBy(NativeLibDependency::coordinates)
      .thenBy(LexicographicIterableComparator()) { it.fileNames }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> = listOf(NativeLibCapability.newInstance(fileNames))
}

@JsonClass(generateAdapter = false)
internal data class ServiceLoaderDependency(
  override val coordinates: Coordinates,
  val providerFile: String,
  val providerClasses: Set<String>,
) : DependencyView<ServiceLoaderDependency> {

  companion object {
    fun newInstance(
      providerFile: String,
      providerClasses: Set<String>,
      artifact: ResolvedArtifactResult,
    ): ServiceLoaderDependency {
      return ServiceLoaderDependency(
        artifact.toCoordinates(),
        providerFile,
        providerClasses.toSortedSet().efficient(),
      )
    }
  }

  override fun compareTo(other: ServiceLoaderDependency): Int {
    return compareBy(ServiceLoaderDependency::coordinates)
      .thenComparing(ServiceLoaderDependency::providerFile)
      .thenBy(LexicographicIterableComparator()) { it.providerClasses }
      .compare(this, other)
  }

  override fun toCapabilities(): List<Capability> =
    listOf(ServiceLoaderCapability.newInstance(providerFile, providerClasses))
}
