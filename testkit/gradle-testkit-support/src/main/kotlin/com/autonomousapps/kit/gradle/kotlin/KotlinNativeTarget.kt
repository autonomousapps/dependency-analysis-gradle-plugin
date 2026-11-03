// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinNativeTarget(
  public val target: String,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("$target()")
  }

  public companion object {
    @JvmStatic
    public fun iosArm64(): KotlinNativeTarget = KotlinNativeTarget("iosArm64")

    @JvmStatic
    public fun iosSimulatorArm64(): KotlinNativeTarget = KotlinNativeTarget("iosSimulatorArm64")

    @JvmStatic
    public fun iosX64(): KotlinNativeTarget = KotlinNativeTarget("iosX64")
  }
}
