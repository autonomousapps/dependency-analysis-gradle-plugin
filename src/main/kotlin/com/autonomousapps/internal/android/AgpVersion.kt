// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.android

import com.android.Version
import com.autonomousapps.internal.utils.VersionNumber

/** @see <a href="https://maven.google.com/web/index.html?#com.android.tools.build:gradle">AGP artifacts</a> */
internal class AgpVersion private constructor(val version: String) : Comparable<AgpVersion> {

  private val versionNumber = VersionNumber.parse(version)

  companion object {

    @JvmStatic val AGP_MIN = version("8.4.2")
    @JvmStatic val AGP_MAX = version("8.13.0")

    @JvmStatic fun current(): AgpVersion = AgpVersion(agpVersion())
    @JvmStatic fun version(version: String): AgpVersion = AgpVersion(version)

    private fun agpVersion(): String = Version.ANDROID_GRADLE_PLUGIN_VERSION
  }

  fun isSupported(): Boolean = current() in AGP_MIN..AGP_MAX ||
    versionNumber.major == AGP_MAX.versionNumber.major &&
    versionNumber.minor == AGP_MAX.versionNumber.minor

  override fun compareTo(other: AgpVersion): Int {
    return if (versionNumber.qualifier?.isNotEmpty() == true && other.versionNumber.qualifier?.isNotEmpty() == true) {
      versionNumber.compareTo(other.versionNumber)
    } else {
      versionNumber.baseVersion.compareTo(other.versionNumber.baseVersion)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AgpVersion

    return versionNumber == other.versionNumber
  }

  override fun hashCode(): Int = versionNumber.hashCode()

  override fun toString(): String = "AgpVersion(versionNumber=$versionNumber)"
}
