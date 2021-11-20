package com.autonomousapps.model.intermediates

import com.autonomousapps.internal.utils.ifNotEmpty
import com.autonomousapps.internal.utils.toCoordinates
import com.autonomousapps.model.*
import org.gradle.api.artifacts.component.ComponentIdentifier

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
 * nb: Deliberate does not implement [DependencyView]. For various reasons, this information gets embedded in
 * [ExplodedJar], which is the preferred access point for deeper analysis.
 */
internal data class AndroidLinterDependency(
  val coordinates: Coordinates,
  val lintRegistry: String
) : Comparable<AndroidLinterDependency> {
  override fun compareTo(other: AndroidLinterDependency): Int = coordinates.compareTo(other.coordinates)
}

/**
 * Metadata from an Android manifest.
 */
internal data class AndroidManifestDependency(
  override val coordinates: Coordinates,
  /** The package name per `<manifest package="...">`. */
  val packageName: String,
  /** A map of component type to components. */
  val componentMap: Map<AndroidManifestCapability.Component, Set<String>>
) : DependencyView<AndroidManifestDependency> {

  constructor(
    packageName: String,
    componentMap: Map<AndroidManifestCapability.Component, Set<String>>,
    componentIdentifier: ComponentIdentifier
  ) : this(
    packageName = packageName,
    componentMap = componentMap,
    coordinates = componentIdentifier.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(AndroidManifestCapability(packageName, componentMap))
}

internal data class AndroidResDependency(
  override val coordinates: Coordinates,
  /** An import that indicates a possible use of an Android resource from this dependency. */
  val import: String,
  val lines: List<AndroidResCapability.Line>
) : DependencyView<AndroidResDependency> {

  override fun toCapabilities(): List<Capability> = listOf(AndroidResCapability(import, lines))
}

internal data class AnnotationProcessorDependency(
  override val coordinates: Coordinates,
  val processor: String,
  val supportedAnnotationTypes: Set<String>
) : DependencyView<AnnotationProcessorDependency> {

  constructor(
    processor: String,
    supportedAnnotationTypes: Set<String>,
    componentIdentifier: ComponentIdentifier
  ) : this(
    processor = processor, supportedAnnotationTypes = supportedAnnotationTypes,
    coordinates = componentIdentifier.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(
    AnnotationProcessorCapability(processor, supportedAnnotationTypes)
  )
}

internal data class InlineMemberDependency(
  override val coordinates: Coordinates,
  val inlineMembers: Set<InlineMemberCapability.InlineMember>
) : DependencyView<InlineMemberDependency> {

  override fun toCapabilities(): List<Capability> = listOf(InlineMemberCapability(inlineMembers))
}

internal data class NativeLibDependency(
  override val coordinates: Coordinates,
  val fileNames: Set<String>
) : DependencyView<NativeLibDependency> {

  override fun toCapabilities(): List<Capability> = listOf(NativeLibCapability(fileNames))
}

internal data class ServiceLoaderDependency(
  override val coordinates: Coordinates,
  val providerFile: String,
  val providerClasses: Set<String>
) : DependencyView<ServiceLoaderDependency> {

  constructor(
    providerFile: String,
    providerClasses: Set<String>,
    componentIdentifier: ComponentIdentifier
  ) : this(
    providerFile = providerFile,
    providerClasses = providerClasses,
    coordinates = componentIdentifier.toCoordinates()
  )

  override fun toCapabilities(): List<Capability> = listOf(ServiceLoaderCapability(providerFile, providerClasses))
}

/**
 * A library or project, along with the set of classes declared by, and other information contained within, this
 * exploded jar. This is the serialized form of [ExplodingJar].
 */
internal data class ExplodedJar(

  override val coordinates: Coordinates,

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
   * The classes declared by this library.
   */
  val classes: Set<String>,
  /**
   * A map of each class declared by this library to the set of constants it defines. The latter may
   * be empty for any given declared class.
   */
  val constantFields: Map<String, Set<String>>,
  /**
   * All of the "Kt" files within this component.
   */
  val ktFiles: Set<KtFile>
) : DependencyView<ExplodedJar> {

  internal constructor(
    artifact: PhysicalArtifact,
    exploding: ExplodingJar
  ) : this(
    coordinates = artifact.coordinates,
    isCompileOnlyAnnotations = exploding.isCompileOnlyCandidate,
    securityProviders = exploding.securityProviders,
    androidLintRegistry = exploding.androidLintRegistry,
    isLintJar = exploding.isLintJar,
    classes = exploding.classNames,
    constantFields = exploding.constants,
    ktFiles = exploding.ktFiles
  )

  init {
    if (isLintJar && androidLintRegistry == null) {
      throw IllegalStateException("Android lint jar for $coordinates must contain a lint registry")
    }
  }

  override fun toCapabilities(): List<Capability> {
    val capabilities = mutableListOf<Capability>()
    capabilities += InferredCapability(isCompileOnlyAnnotations = isCompileOnlyAnnotations)
    classes.ifNotEmpty { capabilities += ClassCapability(it) }
    constantFields.ifNotEmpty { capabilities += ConstantCapability(it, ktFiles) }
    securityProviders.ifNotEmpty { capabilities += SecurityProviderCapability(it) }
    androidLintRegistry?.let { capabilities += AndroidLinterCapability(it, isLintJar) }
    return capabilities
  }
}
