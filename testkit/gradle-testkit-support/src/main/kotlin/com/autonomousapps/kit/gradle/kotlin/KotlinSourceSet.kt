// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.gradle.Dependencies
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinSourceSet(
  override val name: String,
  // TODO(tsr): add a "kotlin { srcDir(...) }" SourceDirectorySet
  private val dependencies: MutableList<Dependency> = mutableListOf(),
) : Element.Block {

  /**
   * KotlinSourceSets should be rendered like this:
   * ```
   * kotlin {
   *   commonMain {
   *     kotlin {
   *       srcDir(...)
   *     }
   *   }
   *
   *   commonMain.dependencies {
   *     api(...)
   *   }
   * }
   * ```
   */
  override fun render(scribe: Scribe): String {
    // TODO(tsr): add `kotlin { ... }` rendering

    return scribe.block("${name}.dependencies") { s ->
      dependencies.forEach { it.render(s) }
    }
  }

  public companion object {
    @JvmStatic
    public fun of(name: String, dependencies: List<Dependency>): KotlinSourceSet {
      return KotlinSourceSet(name, dependencies.toMutableList())
    }

    @JvmStatic
    public fun of(name: String, vararg dependencies: Dependency): KotlinSourceSet {
      return KotlinSourceSet(name, dependencies.toMutableList())
    }
  }
}
