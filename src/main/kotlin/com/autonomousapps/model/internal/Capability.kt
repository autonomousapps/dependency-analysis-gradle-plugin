// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model.internal

import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.internal.utils.mapToOrderedSet
import com.autonomousapps.model.internal.intermediates.consumer.MemberAccess
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

  internal data class PartitionResult(
    val matchingClasses: Set<BinaryClass>,
    val nonMatchingClasses: Set<BinaryClass>,
  ) {

    companion object {
      fun empty(): PartitionResult = PartitionResult(emptySet(), emptySet())
    }

    class Builder {
      val matchingClasses = sortedSetOf<BinaryClass>()
      val nonMatchingClasses = sortedSetOf<BinaryClass>()

      fun build(): PartitionResult {
        return PartitionResult(
          matchingClasses = matchingClasses.efficient(),
          nonMatchingClasses = nonMatchingClasses.efficient(),
        )
      }
    }
  }

  override fun merge(other: Capability): Capability {
    return newInstance(
      binaryClasses = (binaryClasses + (other as BinaryClassCapability).binaryClasses),
    )
  }

  internal fun findMatchingClasses(memberAccess: MemberAccess): PartitionResult {
    val relevant = findRelevantBinaryClasses(memberAccess)

    // lenient
    if (relevant.isEmpty()) return PartitionResult.empty()

    return relevant
      .map { bin -> bin.partition(memberAccess) }
      .fold(PartitionResult.Builder()) { acc, (match, nonMatch) ->
        acc.apply {
          match?.let { matchingClasses.add(it) }
          nonMatch?.let { nonMatchingClasses.add(it) }
        }
      }
      .build()
  }

  /**
   * Example:
   * 1. [memberAccess] is for `groovy/lang/MetaClass#getProperty`.
   * 2. That method is actually provided by `groovy/lang/MetaObjectProtocol`, which `groovy/lang/MetaClass` implements.
   *
   * All of the above ("this" class, its super class, and its interfaces) are relevant for search purposes. Note we
   * don't inspect the member names for this check.
   */
  private fun findRelevantBinaryClasses(memberAccess: MemberAccess): Set<BinaryClass> {
    // direct references
    val relevant = binaryClasses.filterTo(mutableSetOf()) { bin ->
      bin.className == memberAccess.owner
    }

    // Walk up the class hierarchy
    fun walkUp(): Int {
      binaryClasses.filterTo(relevant) { bin ->
        bin.className in relevant.map { it.superClassName }
          || bin.className in relevant.flatMap { it.interfaces }
      }
      return relevant.size
    }

    // TODO(tsr): this could be more performant
    do {
      val size = relevant.size
      val newSize = walkUp()
    } while (newSize > size)

    return relevant
  }

  /**
   * Partitions and returns artificial pair of [BinaryClasses][BinaryClass]. Non-null elements indicate relevant (to
   * [memberAccess] matching and non-matching members of this `BinaryClass`. Matching members are binary-compatible; and
   * non-matching members have the same [name][com.autonomousapps.model.internal.intermediates.producer.Member.name] but
   * incompatible [descriptors][com.autonomousapps.model.internal.intermediates.producer.Member.descriptor], and are
   * therefore binary-incompatible.
   *
   * nb: We don't want this as a method directly in BinaryClass because it can't safely assert the prerequisite that
   * it's only called on "relevant" classes. THIS class, however, can, via findRelevantBinaryClasses.
   */
  private fun BinaryClass.partition(memberAccess: MemberAccess): Pair<BinaryClass?, BinaryClass?> {
    // There can be only one match
    val matchingFields = effectivelyPublicFields.firstOrNull { it.matches(memberAccess) }
    val matchingMethods = effectivelyPublicMethods.firstOrNull { it.matches(memberAccess) }

    // There can be many non-matches
    val nonMatchingFields = effectivelyPublicFields.filterToOrderedSet { it.doesNotMatch(memberAccess) }
    val nonMatchingMethods = effectivelyPublicMethods.filterToOrderedSet { it.doesNotMatch(memberAccess) }

    // Create a view of the binary class containing only the matching members.
    val match = if (matchingFields != null || matchingMethods != null) {
      copy(
        effectivelyPublicFields = matchingFields?.let { setOf(it) }.orEmpty(),
        effectivelyPublicMethods = matchingMethods?.let { setOf(it) }.orEmpty()
      )
    } else {
      null
    }

    // Create a view of the binary class containing only the non-matching members.
    val nonMatch = if (nonMatchingFields.isNotEmpty() || nonMatchingMethods.isNotEmpty()) {
      copy(
        effectivelyPublicFields = nonMatchingFields,
        effectivelyPublicMethods = nonMatchingMethods,
      )
    } else {
      null
    }

    return match to nonMatch
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
