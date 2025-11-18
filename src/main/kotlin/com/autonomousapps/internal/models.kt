// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.strings.binaryToHuman
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.filterNotToSet
import com.autonomousapps.internal.utils.mapToSet
import com.autonomousapps.model.internal.AccessFlags
import com.autonomousapps.model.internal.intermediates.producer.BinaryClass
import com.autonomousapps.model.internal.intermediates.producer.Constant
import com.autonomousapps.model.internal.intermediates.producer.Member
import com.squareup.moshi.JsonClass
import java.lang.annotation.RetentionPolicy
import java.util.regex.Pattern

/** Metadata from an Android manifest. */
internal data class Manifest(
  /** The package name per `<manifest package="...">`. */
  val packageName: String,
  /** A map of component type to components. */
  val componentMap: Map<String, Set<String>>,
) {

  internal enum class Component(val tagName: String, val mapKey: String) {
    ACTIVITY("activity", "activities"),
    SERVICE("service", "services"),
    RECEIVER("receiver", "receivers"),
    PROVIDER("provider", "providers");

    val attrName = "android:name"
  }
}

internal data class AnalyzedClass(
  val className: String,
  val outerClassName: String?,
  val superClassName: String?,
  val retentionPolicy: RetentionPolicy?,
  /**
   * Ignoring constructors and static initializers. Such a class will not prejudice the compileOnly algorithm against
   * declaring the containing jar "annotations-only". See for example `org.jetbrains.annotations.ApiStatus`. This outer
   * class only exists as a sort of "namespace" for the annotations it contains.
   */
  val hasNoMembers: Boolean,
  val access: AccessFlags,
  val methods: Set<Method>,
  val innerClasses: Set<String>,
  val constants: Set<Constant>,
  val reflectiveAccesses: Set<String>,
  val binaryClass: BinaryClass,
) : Comparable<AnalyzedClass> {
  constructor(
    className: String,
    outerClassName: String?,
    superClassName: String?,
    interfaces: Set<String>,
    retentionPolicy: String?,
    isAnnotation: Boolean,
    hasNoMembers: Boolean,
    access: AccessFlags,
    methods: Set<Method>,
    innerClasses: Set<String>,
    constants: Set<Constant>,
    reflectiveAccesses: Set<String>,
    effectivelyPublicFields: Set<Member.Field>,
    effectivelyPublicMethods: Set<Member.Method>,
  ) : this(
    className = className,
    outerClassName = outerClassName,
    superClassName = superClassName,
    retentionPolicy = fromString(retentionPolicy, isAnnotation),
    hasNoMembers = hasNoMembers,
    access = access,
    methods = methods,
    innerClasses = innerClasses,
    constants = constants,
    reflectiveAccesses = reflectiveAccesses,
    binaryClass = BinaryClass(
      className = className.intern(),
      superClassName = superClassName?.intern(),
      interfaces = interfaces.efficient(),
      effectivelyPublicFields = effectivelyPublicFields.efficient(),
      effectivelyPublicMethods = effectivelyPublicMethods.efficient(),
    ),
  )

  companion object {
    fun fromString(name: String?, isAnnotation: Boolean): RetentionPolicy? = when {
      RetentionPolicy.CLASS.name == name -> RetentionPolicy.CLASS
      RetentionPolicy.SOURCE.name == name -> RetentionPolicy.SOURCE
      RetentionPolicy.RUNTIME.name == name -> RetentionPolicy.RUNTIME
      // Default if RetentionPolicy is not specified.
      isAnnotation -> RetentionPolicy.CLASS
      else -> null
    }
  }

  override fun compareTo(other: AnalyzedClass): Int = className.compareTo(other.className)
}

internal data class Method(val types: Set<String>) {

  constructor(descriptor: String) : this(findTypes(descriptor))

  companion object {
    private val DESCRIPTOR = Pattern.compile("L(.+?);")

    private fun findTypes(descriptor: String): Set<String> {
      val types = sortedSetOf<String>()
      val m = DESCRIPTOR.matcher(descriptor)
      while (m.find()) {
        types.add(m.group(1))
      }
      return types.efficient()
    }
  }
}

@JsonClass(generateAdapter = false)
internal data class AbiExclusions(
  val annotationExclusions: Set<String> = emptySet(),
  val classExclusions: Set<String> = emptySet(),
  val pathExclusions: Set<String> = emptySet(),
) {

  @Transient
  private val annotationRegexes = annotationExclusions.mapToSet(String::toRegex)

  @Transient
  private val classRegexes = classExclusions.mapToSet(String::toRegex)

  @Transient
  private val pathRegexes = pathExclusions.mapToSet(String::toRegex)

  fun excludesAnnotation(fqcn: String): Boolean = annotationRegexes.any { it.containsMatchIn(fqcn.binaryToHuman()) }
  fun excludesClass(fqcn: String) = classRegexes.any { it.containsMatchIn(fqcn.binaryToHuman()) }
  fun excludesPath(path: String) = pathRegexes.any { it.containsMatchIn(path) }


  companion object {
    val NONE = AbiExclusions()
  }
}

@JsonClass(generateAdapter = false)
internal data class UsagesExclusions(
  val classExclusions: Set<String> = emptySet(),
) {

  @Transient
  private val classRegexes = classExclusions.mapToSet(String::toRegex)

  private fun excludesClass(fqcn: String) = classRegexes.any { it.containsMatchIn(fqcn.binaryToHuman()) }

  fun excludeClassesFromSet(fqcn: Set<String>): Set<String> {
    return fqcn.filterNotToSet { excludesClass(it) }
  }

  fun excludeClassesFromMap(fqcn: Map<String, String>): Map<String, String> {
    return fqcn.filterNot { excludesClass(it.key) }
  }

  companion object {
    val NONE = UsagesExclusions()
  }
}
