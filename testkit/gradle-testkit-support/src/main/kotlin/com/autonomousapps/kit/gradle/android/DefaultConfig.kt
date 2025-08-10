// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * ```
 * defaultConfig {
 *   applicationId 'com.example' // only for app projects
 *   minSdkVersion 21
 *   targetSdkVersion 29
 *   versionCode 1
 *   versionName '1.0'
 *   testInstrumentationRunner = 'androidx.test.runner.AndroidJUnitRunner'
 * }
 * ```
 */
public class DefaultConfig @JvmOverloads constructor(
  public var applicationId: String? = null,
  public var minSdkVersion: Int,
  public var targetSdkVersion: Int,
  public var versionCode: Int,
  public var versionName: String,
  public var testInstrumentationRunner: String? = null,
) : Element.Block {

  override val name: String = "defaultConfig"

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.block(this) { s ->
    if (applicationId != null) {
      s.line {
        it.append("applicationId ")
        it.appendQuoted(applicationId)
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
      it.append("versionName ")
      it.appendQuoted(versionName)
    }
    if (testInstrumentationRunner != null) {
      s.line {
        it.append("testInstrumentationRunner = ")
        it.appendQuoted(testInstrumentationRunner)
      }
    }
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.block(this) { s ->
    if (applicationId != null) {
      s.line {
        it.append("applicationId = ")
        it.appendQuoted(applicationId)
      }
    }
    s.line {
      it.append("minSdk = ")
      it.append(minSdkVersion)
    }
    s.line {
      it.append("targetSdk = ")
      it.append(targetSdkVersion)
    }
    s.line {
      it.append("versionCode = ")
      it.append(versionCode)
    }
    s.line {
      it.append("versionName = ")
      it.appendQuoted(versionName)
    }
    if (testInstrumentationRunner != null) {
      s.line {
        it.append("testInstrumentationRunner = ")
        it.appendQuoted(testInstrumentationRunner)
      }
    }
  }

  public class Builder {
    public var applicationId: String? = null
    public var minSdkVersion: Int? = null
    public var targetSdkVersion: Int? = null
    public var versionCode: Int? = null
    public var versionName: String? = null
    public var testInstrumentationRunner: String? = null

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
        testInstrumentationRunner = testInstrumentationRunner,
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

    @JvmField
    public val DEFAULT_TEST: DefaultConfig = DefaultConfig(
      minSdkVersion = 21,
      targetSdkVersion = 29,
      versionCode = 1,
      versionName = "1.0",
    )
  }
}
