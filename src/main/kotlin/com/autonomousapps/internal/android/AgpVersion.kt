package com.autonomousapps.internal.android

import com.android.Version
import com.autonomousapps.internal.utils.VersionNumber

internal class AgpVersion private constructor(val version: String) : Comparable<AgpVersion> {

  private val versionNumber = VersionNumber.parse(version)

  companion object {

    @JvmStatic val AGP_MIN = version("4.2.2")
    @JvmStatic val AGP_MAX = version("7.4.1")

    @JvmStatic fun current(): AgpVersion = AgpVersion(agpVersion())
    @JvmStatic fun version(version: String): AgpVersion = AgpVersion(version)

    private fun agpVersion(): String = Version.ANDROID_GRADLE_PLUGIN_VERSION
  }

  fun isSupported(): Boolean = current() in AGP_MIN..AGP_MAX

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

    if (versionNumber != other.versionNumber) return false

    return true
  }

  override fun hashCode(): Int = versionNumber.hashCode()

  override fun toString(): String = "AgpVersion(versionNumber=$versionNumber)"
}
