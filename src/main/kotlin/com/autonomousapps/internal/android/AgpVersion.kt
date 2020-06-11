package com.autonomousapps.internal.android

import org.gradle.util.VersionNumber

/**
 * A wrapper around [VersionNumber].
 */
internal class AgpVersion private constructor(val version: String) : Comparable<AgpVersion> {

  private val versionNumber = VersionNumber.parse(version)

  companion object {

    val AGP_MIN = version("3.5.3")
    val AGP_MAX = version("4.1.0-beta01")

    @JvmStatic fun current(): AgpVersion = AgpVersion(agpVersion())
    @JvmStatic fun version(version: String): AgpVersion = AgpVersion(version)

    @Suppress("DEPRECATION")
    private fun agpVersion(): String {
      return try {
        // AGP 3.6+
        com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
      } catch (_: Throwable) {
        // AGP 3.5.3. This is deprecated in 4+ (removed in 5).
        com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
      }
    }
  }

  fun isSupported(): Boolean = current() in AGP_MIN..AGP_MAX

  // versionNumber.qualifier is in fact nullable. Kotlin is totally wrong on this.
  @Suppress("SimplifyBooleanWithConstants", "UNNECESSARY_SAFE_CALL")
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

    // != refuses to compile??
    @Suppress("ReplaceCallWithBinaryOperator")
    if (!versionNumber.equals(other.versionNumber)) return false

    return true
  }

  override fun hashCode(): Int = versionNumber.hashCode()

  override fun toString(): String = "AgpVersion(versionNumber=$versionNumber)"
}