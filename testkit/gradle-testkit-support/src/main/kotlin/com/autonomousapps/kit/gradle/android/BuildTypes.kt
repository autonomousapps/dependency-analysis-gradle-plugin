// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * ```
 * buildTypes {
 *   debug {
 *     signingConfig signingConfigs.debug
 *     applicationIdSuffix ".debug"
 *   }
 *   release {
 *     signingConfig signingConfigs.release
 *   }
 * }
 * ```
 */
public class BuildTypes(public val buildTypes: MutableList<BuildType>) : Element.Block {

  override val name: String = "buildTypes"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    buildTypes.forEach { it.render(s) }
  }

  public operator fun plus(other: BuildTypes): BuildTypes {
    return BuildTypes((buildTypes + other.buildTypes).distinct().toMutableList())
  }

  public operator fun plus(other: Iterable<BuildType>): BuildTypes {
    return BuildTypes((buildTypes + other).distinct().toMutableList())
  }

  public companion object {
    @JvmStatic
    public fun of(vararg buildTypes: BuildType): BuildTypes {
      return BuildTypes(buildTypes.toMutableList())
    }

    @JvmStatic
    public fun of(buildTypes: Iterable<BuildType>): BuildTypes {
      return BuildTypes(buildTypes.toMutableList())
    }

    @JvmStatic
    public fun ofNames(vararg buildTypeNames: String): BuildTypes {
      return BuildTypes(buildTypeNames.map { BuildType(it) }.toMutableList())
    }

    @JvmStatic
    public fun ofNames(buildTypeNames: Iterable<String>): BuildTypes {
      return BuildTypes(buildTypeNames.map { BuildType(it) }.toMutableList())
    }
  }
}
