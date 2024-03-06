// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import java.util.Objects

/**
 * From `org.gradle.util.VersionNumber` (deprecated in 7, removed in 8).
 */
@Suppress("unused", "VARIABLE_WITH_REDUNDANT_INITIALIZER") // converted from Java
class VersionNumber private constructor(
  val major: Int,
  val minor: Int,
  val micro: Int,
  val patch: Int,
  val qualifier: String?,
  private val scheme: AbstractScheme,
) : Comparable<VersionNumber> {

  constructor(
    major: Int,
    minor: Int,
    micro: Int,
    qualifier: String?,
  ) : this(
    major = major,
    minor = minor,
    micro = micro,
    patch = 0,
    qualifier = qualifier,
    scheme = DEFAULT_SCHEME
  )

  constructor(
    major: Int,
    minor: Int,
    micro: Int,
    patch: Int,
    qualifier: String?,
  ) : this(
    major = major,
    minor = minor,
    micro = micro,
    patch = patch,
    qualifier = qualifier,
    scheme = PATCH_SCHEME
  )

  companion object {
    val UNKNOWN = version(0)
    private val DEFAULT_SCHEME = DefaultScheme()
    private val PATCH_SCHEME = SchemeWithPatchVersion()

    fun version(major: Int): VersionNumber {
      return version(major, 0)
    }

    fun version(major: Int, minor: Int): VersionNumber {
      return VersionNumber(major, minor, 0, 0, null, DEFAULT_SCHEME)
    }

    fun parse(versionString: String?): VersionNumber {
      return DEFAULT_SCHEME.parse(versionString)
    }

    private fun toLowerCase(string: String?): String? = string?.lowercase()

    /**
     * Returns the default MAJOR.MINOR.MICRO-QUALIFIER scheme.
     */
    fun scheme(): Scheme {
      return DEFAULT_SCHEME
    }

    /**
     * Returns the MAJOR.MINOR.MICRO.PATCH-QUALIFIER scheme.
     */
    fun withPatchNumber(): Scheme {
      return PATCH_SCHEME
    }
  }

  val baseVersion: VersionNumber
    get() = VersionNumber(major, minor, micro, patch, null, scheme)

  override fun compareTo(other: VersionNumber): Int {
    if (major != other.major) return major - other.major
    if (minor != other.minor) return minor - other.minor
    if (micro != other.minor) return micro - other.micro
    if (patch != other.patch) return patch - other.patch

    return nullsLast<String>().compare(
      qualifier?.lowercase(),
      other.qualifier?.lowercase()
    )
  }

  override fun equals(other: Any?): Boolean {
    return other is VersionNumber && compareTo(other) == 0
  }

  override fun hashCode(): Int {
    var result = major
    result = 31 * result + minor
    result = 31 * result + micro
    result = 31 * result + patch
    result = 31 * result + Objects.hashCode(qualifier)
    return result
  }

  override fun toString(): String {
    return scheme.format(this)
  }

  interface Scheme {
    fun parse(versionString: String?): VersionNumber
    fun format(versionNumber: VersionNumber): String
  }

  private abstract class AbstractScheme protected constructor(val depth: Int) : Scheme {
    override fun parse(versionString: String?): VersionNumber {
      if (versionString.isNullOrEmpty()) return UNKNOWN

      val scanner = Scanner(versionString)
      var major = 0
      var minor = 0
      var micro = 0
      var patch = 0

      if (!scanner.hasDigit()) return UNKNOWN

      major = scanner.scanDigit()
      if (scanner.isSeparatorAndDigit('.')) {
        scanner.skipSeparator()
        minor = scanner.scanDigit()
        if (scanner.isSeparatorAndDigit('.')) {
          scanner.skipSeparator()
          micro = scanner.scanDigit()
          if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
            scanner.skipSeparator()
            patch = scanner.scanDigit()
          }
        }
      }
      if (scanner.isEnd()) {
        return VersionNumber(major, minor, micro, patch, null, this)
      }
      if (scanner.isQualifier()) {
        scanner.skipSeparator()
        return VersionNumber(major, minor, micro, patch, scanner.remainder(), this)
      }
      return UNKNOWN
    }

    private class Scanner(val str: String) {
      var pos = 0
      fun hasDigit(): Boolean {
        return pos < str.length && Character.isDigit(str[pos])
      }

      fun isSeparatorAndDigit(vararg separators: Char): Boolean {
        return pos < str.length - 1 && oneOf(*separators) && Character.isDigit(str[pos + 1])
      }

      private fun oneOf(vararg separators: Char): Boolean {
        val current = str[pos]
        for (i in separators.indices) {
          val separator = separators[i]
          if (current == separator) {
            return true
          }
        }
        return false
      }

      fun isQualifier(): Boolean = pos < str.length - 1 && oneOf('.', '-')

      fun scanDigit(): Int {
        val start = pos
        while (hasDigit()) {
          pos++
        }
        return str.substring(start, pos).toInt()
      }

      fun isEnd(): Boolean = pos == str.length

      fun skipSeparator() {
        pos++
      }

      fun remainder(): String? {
        return if (pos == str.length) null else str.substring(pos)
      }
    }
  }

  private class DefaultScheme : AbstractScheme(3) {
    override fun format(versionNumber: VersionNumber): String {
      return String.format(
        VERSION_TEMPLATE, versionNumber.major, versionNumber.minor, versionNumber.micro,
        if (versionNumber.qualifier == null) "" else "-" + versionNumber.qualifier
      )
    }

    companion object {
      private const val VERSION_TEMPLATE = "%d.%d.%d%s"
    }
  }

  private class SchemeWithPatchVersion : AbstractScheme(4) {
    override fun format(versionNumber: VersionNumber): String {
      return String.format(
        VERSION_TEMPLATE, versionNumber.major, versionNumber.minor, versionNumber.micro, versionNumber.patch,
        if (versionNumber.qualifier == null) "" else "-" + versionNumber.qualifier
      )
    }

    companion object {
      private const val VERSION_TEMPLATE = "%d.%d.%d.%d%s"
    }
  }
}
