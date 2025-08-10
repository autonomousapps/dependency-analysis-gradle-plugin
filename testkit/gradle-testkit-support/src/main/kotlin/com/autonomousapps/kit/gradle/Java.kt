// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Java @JvmOverloads constructor(
  public val features: List<Feature> = emptyList(),
  public val toolchain: Toolchain? = null,
) : Element.Block {

  override val name: String = "java"

  /**
   * Example:
   * ```
   * java {
   *   registerFeature("magic") {
   *     usingSourceSet(sourceSets["magic"])
   *   }
   *   toolchain {
   *     languageVersion.set(JavaLanguageVersion.of(17))
   *   }
   * }
   * ```
   */
  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    features.forEach { it.render(s) }
    toolchain?.render(s)
  }

  public companion object {
    @JvmStatic
    public fun of(toolchain: Toolchain, vararg features: String): Java = Java(
      features.map { Feature.ofName(it) },
      toolchain,
    )

    @JvmStatic
    public fun of(javaLanguageVersion: Int, vararg features: String): Java = Java(
      features.map { Feature.ofName(it) },
      Toolchain(javaLanguageVersion),
    )

    @JvmStatic
    public fun ofFeatures(features: List<Feature>): Java = Java(features)

    @JvmStatic
    public fun ofFeatures(vararg features: Feature): Java = ofFeatures(features.toList())

    @JvmStatic
    public fun ofFeatures(vararg features: String): Java = ofFeatures(features.map { Feature.ofName(it) })
  }
}
