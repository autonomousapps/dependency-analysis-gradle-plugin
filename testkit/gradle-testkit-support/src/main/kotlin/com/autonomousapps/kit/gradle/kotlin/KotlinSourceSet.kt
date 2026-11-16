// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

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
public class KotlinSourceSet @JvmOverloads constructor(
  override val name: String,
  private val get: Boolean = false,
  private val dependencies: MutableList<Dependency> = mutableListOf(),
  private val srcDirs: List<String>,
) : Element.Block {

  override fun render(scribe: Scribe): String {
    val name = if (get) "getByName(\"$name\")" else name
    return scribe.block(name) { s ->
      if (dependencies.isNotEmpty()) {
        s.block("dependencies") {
          dependencies.forEach { dependency -> dependency.render(s) }
        }
      }

      if (srcDirs.isNotEmpty()) {
        s.block("kotlin") {
          srcDirs.forEach { srcDir ->
            s.line { s.append("srcDir(\"$srcDir\")") }
          }
        }
      }
    }
  }

  public class Builder(
    private val name: String,
    private val get: Boolean = false,
  ) {
    private val dependencies = mutableListOf<Dependency>()
    private val srcDirs = mutableListOf<String>()

    public fun dependencies(vararg dependencies: Dependency) {
      this.dependencies.addAll(dependencies)
    }

    public fun srcDirs(vararg srcDirs: String) {
      this.srcDirs.addAll(srcDirs)
    }

    public fun build(): KotlinSourceSet {
      return KotlinSourceSet(
        name = name,
        get = get,
        dependencies = dependencies,
        srcDirs = srcDirs,
      )
    }
  }
}
