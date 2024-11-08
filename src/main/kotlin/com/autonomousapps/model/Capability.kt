// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.autonomousapps.internal.utils.filterToOrderedSet
import com.autonomousapps.model.intermediates.consumer.MemberAccess
import com.autonomousapps.model.intermediates.producer.BinaryClass
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
sealed class Capability : Comparable<Capability> {
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
data class AndroidLinterCapability(
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
data class AndroidManifestCapability(
  val componentMap: Map<Component, Set<String>>,
) : Capability() {

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
    return AndroidManifestCapability(componentMap + (other as AndroidManifestCapability).componentMap)
  }
}

@TypeLabel("asset")
@JsonClass(generateAdapter = false)
data class AndroidAssetCapability(
  val assets: List<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return AndroidAssetCapability(assets + (other as AndroidAssetCapability).assets)
  }
}

@TypeLabel("res")
@JsonClass(generateAdapter = false)
data class AndroidResCapability(
  val rImport: String,
  val lines: List<Line>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class Line(val type: String, val value: String)

  override fun merge(other: Capability): Capability {
    return AndroidResCapability(
      rImport + (other as AndroidResCapability).rImport,
      lines + other.lines
    )
  }
}

@TypeLabel("proc")
@JsonClass(generateAdapter = false)
data class AnnotationProcessorCapability(
  val processor: String,
  val supportedAnnotationTypes: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return AnnotationProcessorCapability(
      processor, // other.processor ?
      supportedAnnotationTypes + (other as AnnotationProcessorCapability).supportedAnnotationTypes
    )
  }
}

@TypeLabel("binaryClass")
@JsonClass(generateAdapter = false)
data class BinaryClassCapability(
  val binaryClasses: Set<BinaryClass>,
) : Capability() {

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
          matchingClasses = matchingClasses,
          nonMatchingClasses = nonMatchingClasses,
        )
      }
    }
  }

  override fun merge(other: Capability): Capability {
    return BinaryClassCapability(
      binaryClasses = binaryClasses + (other as BinaryClassCapability).binaryClasses,
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
   * non-matching members have the same [name][com.autonomousapps.model.intermediates.producer.Member.name] but
   * incompatible [descriptors][com.autonomousapps.model.intermediates.producer.Member.descriptor], and are therefore
   * binary-incompatible.
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

@TypeLabel("class")
@JsonClass(generateAdapter = false)
data class ClassCapability(
  val classes: Set<String>,
) : Capability() {

  override fun merge(other: Capability): Capability {
    return ClassCapability(classes + (other as ClassCapability).classes)
  }
}

@TypeLabel("const")
@JsonClass(generateAdapter = false)
data class ConstantCapability(
  /** Map of fully-qualified class names to constant field names. */
  val constants: Map<String, Set<String>>,
  /** Kotlin classes with top-level declarations. */
  val ktFiles: Set<KtFile>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return ConstantCapability(
      constants + (other as ConstantCapability).constants,
      ktFiles + other.ktFiles
    )
  }
}

@TypeLabel("inferred")
@JsonClass(generateAdapter = false)
data class InferredCapability(
  /**
   * True if this dependency contains only annotations that are only needed at compile-time (`CLASS` and `SOURCE` level
   * retention policies). False otherwise.
   */
  val isCompileOnlyAnnotations: Boolean,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return InferredCapability(isCompileOnlyAnnotations && (other as InferredCapability).isCompileOnlyAnnotations)
  }
}

@TypeLabel("inline")
@JsonClass(generateAdapter = false)
data class InlineMemberCapability(
  val inlineMembers: Set<InlineMember>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class InlineMember(
    val packageName: String,
    val inlineMembers: Set<String>,
  ) : Comparable<InlineMember> {
    override fun compareTo(other: InlineMember): Int = compareBy(InlineMember::packageName)
      .thenBy(LexicographicIterableComparator()) { it.inlineMembers }
      .compare(this, other)
  }

  override fun merge(other: Capability): Capability {
    return InlineMemberCapability(inlineMembers + (other as InlineMemberCapability).inlineMembers)
  }
}

@TypeLabel("typealias")
@JsonClass(generateAdapter = false)
data class TypealiasCapability(
  val typealiases: Set<Typealias>,
) : Capability() {

  @JsonClass(generateAdapter = false)
  data class Typealias(
    val packageName: String,
    val typealiases: Set<Alias>,
  ) : Comparable<Typealias> {

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
    return TypealiasCapability(typealiases + (other as TypealiasCapability).typealiases)
  }
}

@TypeLabel("native")
@JsonClass(generateAdapter = false)
data class NativeLibCapability(
  val fileNames: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return NativeLibCapability(fileNames + (other as NativeLibCapability).fileNames)
  }
}

@TypeLabel("service_loader")
@JsonClass(generateAdapter = false)
data class ServiceLoaderCapability(
  val providerFile: String,
  val providerClasses: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return ServiceLoaderCapability(
      providerFile + (other as ServiceLoaderCapability).providerFile, providerClasses + other.providerClasses
    )
  }
}

@TypeLabel("security_provider")
@JsonClass(generateAdapter = false)
data class SecurityProviderCapability(
  val securityProviders: Set<String>,
) : Capability() {
  override fun merge(other: Capability): Capability {
    return SecurityProviderCapability(securityProviders + (other as SecurityProviderCapability).securityProviders)
  }
}
