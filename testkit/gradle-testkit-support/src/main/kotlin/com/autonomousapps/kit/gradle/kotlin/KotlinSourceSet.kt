// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinSourceSet @JvmOverloads constructor(
  override val name: String,
  private val get: Boolean = false,
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
   *
   *   androidMain.dependencies { ... }
   *
   *   getByName("androidHostTest").dependencies { ... }
   * }
   * ```
   */
  override fun render(scribe: Scribe): String {
    // TODO(tsr): add `kotlin { ... }` rendering

    val name = if (get) "getByName(\"$name\")" else name
    return scribe.block("${name}.dependencies") { s ->
      dependencies.forEach { it.render(s) }
    }
  }

  public class Builder(
    private val name: String,
    private val get: Boolean = false,
  ) {
    private val dependencies = mutableListOf<Dependency>()

    public fun dependencies(vararg dependencies: Dependency) {
      this.dependencies.addAll(dependencies)
    }

    public fun build(): KotlinSourceSet {
      return KotlinSourceSet(
        name = name,
        get = get,
        dependencies = dependencies,
      )
    }
  }
}
