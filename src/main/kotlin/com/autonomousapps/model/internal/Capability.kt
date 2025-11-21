// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.Constant
import com.autonomousapps.model.internal.intermediates.producer.ReflectingDependency
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
internal sealed class Capability : Comparable<Capability> {
  override fun compareTo(other: Capability): Int = javaClass.simpleName.compareTo(other.javaClass.simpleName)

  /**
   * This is for the JVM world, where sometimes multiple Jar files make up one component.
   * Subclasses implement this to merge details in a useful way.
   * It's implemented in all subclasses for completeness, although some situations might never occur in Android.
   */
  abstract fun merge(other: Capability): Capability
}

@TypeLabel("linter")
@JsonClass(generateAdapter = false)
internal data class AndroidLinterCapability(
  val lintRegistry: String,
  /** True if this dependency contains _only_ an Android lint jar/registry. */
  val isLintJar: Boolean,
) : Capability() {

  override fun merge(other: Capability): Capability {
    return AndroidLinterCapability(lintRegistry, isLintJar && (other as AndroidLinterCapability).isLintJar)
  }
}

@TypeLabel("manifest")
@JsonClass(generateAdapter = false)
internal data class AndroidManifestCapability(
  val componentMap: Map<Component, Set<String>>,
) : Capability() {

  companion object {
    fun newInstance(componentMap: Map<Component, Set<String>>): AndroidManifestCapability {
      return AndroidManifestCapability(componentMap.toSortedMap().efficient())
    }
  }

  enum class Component(val mapKey: String) {
    ACTIVITY("activities"),
    SERVICE("services"),
    RECEIVER("receivers"),
    PROVIDER("providers");

    companion object {
      internal fun of(mapKey: String): Component {
        return values().find {
          it.mapKey == mapKey
        } ?: error("Could not find Manifest.Component for $mapKey")
      }
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance((componentMap + (other as AndroidManifestCapability).componentMap))
  }
}

@TypeLabel("asset")
@JsonClass(generateAdapter = false)
internal data class AndroidAssetCapability(
  val assets: List<String>,
) : Capability() {

  companion object {
    fun newInstance(assets: List<String>): AndroidAssetCapability {
      return AndroidAssetCapability(assets.sorted().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance((assets + (other as AndroidAssetCapability).assets))
  }
}

@TypeLabel("res")
@JsonClass(generateAdapter = false)
internal data class AndroidResCapability(
  val rImport: String,
  val lines: List<Line>,
) : Capability() {

  companion object {
    fun newInstance(rImport: String, lines: List<Line>): AndroidResCapability {
      return AndroidResCapability(rImport, lines.sorted().efficient())
    }
  }

  @JsonClass(generateAdapter = false)
  data class Line(val type: String, val value: String) : Comparable<Line> {
    override fun compareTo(other: Line): Int = compareBy(Line::type)
      .thenComparing(compareBy(Line::value))
      .compare(this, other)
  }

  override fun merge(other: Capability): Capability {
    return newInstance(
      rImport + (other as AndroidResCapability).rImport, // TODO(tsr): this seems wrong
      (lines + other.lines)
    )
  }
}

@TypeLabel("proc")
@JsonClass(generateAdapter = false)
internal data class AnnotationProcessorCapability(
  val processor: String,
  val supportedAnnotationTypes: Set<String>,
) : Capability() {

  companion object {
    fun newInstance(processor: String, supportedAnnotationTypes: Set<String>): AnnotationProcessorCapability {
      return AnnotationProcessorCapability(processor, supportedAnnotationTypes.toSortedSet().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance(
      processor, // other.processor ?
      (supportedAnnotationTypes + (other as AnnotationProcessorCapability).supportedAnnotationTypes)
    )
  }
}

@TypeLabel("binaryClass")
@JsonClass(generateAdapter = false)
internal data class BinaryClassCapability(
  val binaryClasses: Set<BinaryClass>,
) : Capability() {

  companion object {
    fun newInstance(binaryClasses: Set<BinaryClass>): BinaryClassCapability {
      return BinaryClassCapability(binaryClasses.toSortedSet().efficient())
    }
  }

  val classes by unsafeLazy { binaryClasses.mapToOrderedSet { it.className } }

  override fun merge(other: Capability): Capability {
    return newInstance(
      binaryClasses = (binaryClasses + (other as BinaryClassCapability).binaryClasses).efficient(),
    )
  }
}

@TypeLabel("const")
@JsonClass(generateAdapter = false)
internal data class ConstantCapability(
  /** Map of fully-qualified class names to constant fields. */
  val constants: Map<String, Set<Constant>>,
  /** Kotlin classes with top-level declarations. */
  val ktFiles: Set<KtFile>,
) : Capability() {

  companion object {
    fun newInstance(constants: Map<String, Set<Constant>>, ktFiles: Set<KtFile>): ConstantCapability {
      return ConstantCapability(constants.toSortedMap().efficient(), ktFiles.toSortedSet().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance((constants + (other as ConstantCapability).constants), (ktFiles + other.ktFiles))
  }
}

@TypeLabel("inferred")
@JsonClass(generateAdapter = false)
internal data class InferredCapability(
  /** True if this dependency contains only annotations. False otherwise. */
  val isAnnotations: Boolean = false,
  /** All reflective accesses _of_ this dependency. May be empty. */
  val reflectiveAccesses: Set<ReflectingDependency.ReflectiveAccess> = emptySet(),
) : Capability() {

  override fun merge(other: Capability): Capability {
    val accesses = reflectiveAccesses + (other as InferredCapability).reflectiveAccesses.toSortedSet().efficient()
    return InferredCapability(
      isAnnotations = isAnnotations || other.isAnnotations,
      reflectiveAccesses = accesses,
    )
  }
}

@TypeLabel("inline")
@JsonClass(generateAdapter = false)
internal data class InlineMemberCapability(
  val inlineMembers: Set<InlineMember>,
) : Capability() {

  companion object {
    fun newInstance(inlineMembers: Set<InlineMember>): InlineMemberCapability {
      return InlineMemberCapability(inlineMembers.toSortedSet().efficient())
    }
  }

  @JsonClass(generateAdapter = false)
  data class InlineMember(
    val packageName: String,
    val inlineMembers: Set<String>,
  ) : Comparable<InlineMember> {

    companion object {
      fun newInstance(packageName: String, inlineMembers: Set<String>): InlineMember {
        return InlineMember(packageName, inlineMembers.toSortedSet().efficient())
      }
    }

    override fun compareTo(other: InlineMember): Int = compareBy(InlineMember::packageName)
      .thenBy(LexicographicIterableComparator()) { it.inlineMembers }
      .compare(this, other)
  }

  override fun merge(other: Capability): Capability {
    return newInstance((inlineMembers + (other as InlineMemberCapability).inlineMembers))
  }
}

@TypeLabel("typealias")
@JsonClass(generateAdapter = false)
internal data class TypealiasCapability(
  val typealiases: Set<Typealias>,
) : Capability() {

  companion object {
    fun newInstance(typealiases: Set<Typealias>): TypealiasCapability {
      return TypealiasCapability(typealiases.toSortedSet().efficient())
    }
  }

  @JsonClass(generateAdapter = false)
  data class Typealias(
    val packageName: String,
    val typealiases: Set<Alias>,
  ) : Comparable<Typealias> {

    companion object {
      fun newInstance(packageName: String, typealiases: Set<Alias>): Typealias {
        return Typealias(packageName, typealiases.toSortedSet().efficient())
      }
    }

    override fun compareTo(other: Typealias): Int = compareBy(Typealias::packageName)
      .thenBy(LexicographicIterableComparator()) { it.typealiases }
      .compare(this, other)

    @JsonClass(generateAdapter = false)
    data class Alias(
      val name: String,
      val expandedType: String,
    ) : Comparable<Alias> {
      override fun compareTo(other: Alias): Int = compareBy(Alias::name)
        .thenComparing(Alias::expandedType)
        .compare(this, other)
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance((typealiases + (other as TypealiasCapability).typealiases))
  }
}

@TypeLabel("native")
@JsonClass(generateAdapter = false)
internal data class NativeLibCapability(
  val fileNames: Set<String>,
) : Capability() {

  companion object {
    fun newInstance(fileNames: Set<String>): NativeLibCapability {
      return NativeLibCapability(fileNames.toSortedSet().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance((fileNames + (other as NativeLibCapability).fileNames))
  }
}

@TypeLabel("service_loader")
@JsonClass(generateAdapter = false)
internal data class ServiceLoaderCapability(
  val providerFile: String,
  val providerClasses: Set<String>,
) : Capability() {

  companion object {
    fun newInstance(providerFile: String, providerClasses: Set<String>): ServiceLoaderCapability {
      return ServiceLoaderCapability(providerFile, providerClasses.toSortedSet().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance(
      providerFile + (other as ServiceLoaderCapability).providerFile,
      (providerClasses + other.providerClasses),
    )
  }
}

@TypeLabel("security_provider")
@JsonClass(generateAdapter = false)
internal data class SecurityProviderCapability(
  val securityProviders: Set<String>,
) : Capability() {

  companion object {
    fun newInstance(securityProviders: Set<String>): SecurityProviderCapability {
      return SecurityProviderCapability(securityProviders.toSortedSet().efficient())
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance(
      (securityProviders + (other as SecurityProviderCapability).securityProviders),
    )
  }
}
