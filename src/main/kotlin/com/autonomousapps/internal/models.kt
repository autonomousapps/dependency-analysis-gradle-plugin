// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal

import com.autonomousapps.internal.asm.Opcodes
import com.autonomousapps.internal.utils.efficient
import com.autonomousapps.internal.utils.filterNotToSet
import com.autonomousapps.internal.utils.mapToSet
import com.squareup.moshi.JsonClass
import java.lang.annotation.RetentionPolicy
import java.util.regex.Pattern

/** Metadata from an Android manifest. */
data class Manifest(
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

data class AnalyzedClass(
  val className: String,
  val outerClassName: String?,
  val superClassName: String?,
  val retentionPolicy: RetentionPolicy?,
  /**
   * Ignoring constructors and static initializers. Such a class will not prejudice the compileOnly
   * algorithm against declaring the containing jar "annotations-only". See for example
   * `org.jetbrains.annotations.ApiStatus`. This outer class only exists as a sort of "namespace"
   * for the annotations it contains.
   */
  val hasNoMembers: Boolean,
  val access: Access,
  val methods: Set<Method>,
  val innerClasses: Set<String>,
  val constantFields: Set<String>,
) : Comparable<AnalyzedClass> {

  constructor(
    className: String,
    outerClassName: String?,
    superClassName: String?,
    retentionPolicy: String?,
    isAnnotation: Boolean,
    hasNoMembers: Boolean,
    access: Access,
    methods: Set<Method>,
    innerClasses: Set<String>,
    constantClasses: Set<String>,
  ) : this(
    className = className,
    outerClassName = outerClassName,
    superClassName = superClassName,
    retentionPolicy = fromString(retentionPolicy, isAnnotation),
    hasNoMembers = hasNoMembers,
    access = access,
    methods = methods,
    innerClasses = innerClasses,
    constantFields = constantClasses
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

enum class Access {
  PUBLIC,
  PROTECTED,
  PRIVATE,
  PACKAGE_PRIVATE;

  companion object {
    fun fromInt(access: Int): Access {
      return when {
        isPublic(access) -> PUBLIC
        isProtected(access) -> PROTECTED
        isPrivate(access) -> PRIVATE
        isPackagePrivate(access) -> PACKAGE_PRIVATE
        else -> throw IllegalArgumentException("Access <$access> is an unknown value")
      }
    }

    private fun isPackagePrivate(access: Int): Boolean =
      !isPublic(access) && !isPrivate(access) && !isProtected(access)

    private fun isPublic(access: Int): Boolean = access and Opcodes.ACC_PUBLIC != 0

    private fun isPrivate(access: Int): Boolean = access and Opcodes.ACC_PRIVATE != 0

    private fun isProtected(access: Int): Boolean = access and Opcodes.ACC_PROTECTED != 0
  }
}

data class Method internal constructor(val types: Set<String>) {

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

  fun excludesAnnotation(fqcn: String): Boolean = annotationRegexes.any { it.containsMatchIn(fqcn.dotty()) }
  fun excludesClass(fqcn: String) = classRegexes.any { it.containsMatchIn(fqcn.dotty()) }
  fun excludesPath(path: String) = pathRegexes.any { it.containsMatchIn(path) }

  // The user-facing regex expects FQCNs to be delimited with dots, not slashes
  private fun String.dotty() = replace('/', '.').removeSurrounding("L", ";")

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

  private fun excludesClass(fqcn: String) = classRegexes.any { it.containsMatchIn(fqcn.dotty()) }

  fun excludeClassesFromSet(fqcn: Set<String>): Set<String> {
    return fqcn.filterNotToSet { excludesClass(it) }
  }

  // The user-facing regex expects FQCNs to be delimited with dots, not slashes
  private fun String.dotty() = replace('/', '.').removeSurrounding("L", ";")

  companion object {
    val NONE = UsagesExclusions()
  }
}
