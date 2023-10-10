package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * ```
 * defaultConfig {
 *   applicationId "com.example" // only for app projects
 *   minSdkVersion 21
 *   targetSdkVersion 29
 *   versionCode 1
 *   versionName "1.0"
 * }
 * ```
 */
public class DefaultConfig @JvmOverloads constructor(
  private val applicationId: String? = null,
  private val minSdkVersion: Int,
  private val targetSdkVersion: Int,
  private val versionCode: Int,
  private val versionName: String,
) : Element.Block {

  override val name: String = "defaultConfig"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    if (applicationId != null) {
      s.line {
        it.append("applicationId \"")
        it.append(applicationId)
        it.append("\"")
      }
    }
    s.line {
      it.append("minSdkVersion ")
      it.append(minSdkVersion)
    }
    s.line {
      it.append("targetSdkVersion ")
      it.append(targetSdkVersion)
    }
    s.line {
      it.append("versionCode ")
      it.append(versionCode)
    }
    s.line {
      it.append("versionName \"")
      it.append(versionName)
      it.append("\"")
    }
  }

  public class Builder {
    public var applicationId: String? = null
    public var minSdkVersion: Int? = null
    public var targetSdkVersion: Int? = null
    public var versionCode: Int? = null
    public var versionName: String? = null

    public fun build(): DefaultConfig {
      val minSdkVersion = checkNotNull(minSdkVersion)
      val targetSdkVersion = checkNotNull(targetSdkVersion)
      val versionCode = checkNotNull(versionCode)
      val versionName = checkNotNull(versionName)

      return DefaultConfig(
        applicationId = applicationId,
        minSdkVersion = minSdkVersion,
        targetSdkVersion = targetSdkVersion,
        versionCode = versionCode,
        versionName = versionName,
      )
    }
  }

  public companion object {
    @JvmField
    public val DEFAULT_APP: DefaultConfig = DefaultConfig(
      applicationId = "com.example",
      minSdkVersion = 21,
      targetSdkVersion = 29,
      versionCode = 1,
      versionName = "1.0",
    )

    @JvmField
    public val DEFAULT_LIB: DefaultConfig = DefaultConfig(
      minSdkVersion = 21,
      targetSdkVersion = 29,
      versionCode = 1,
      versionName = "1.0",
    )
  }
}
