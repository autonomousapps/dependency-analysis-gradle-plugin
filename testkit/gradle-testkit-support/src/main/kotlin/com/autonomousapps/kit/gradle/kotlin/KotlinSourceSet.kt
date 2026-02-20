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
  private val dependencies: Dependencies,
) : Element.Block {

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    dependencies.render(s)
  }

  public companion object {
    @JvmStatic
    public fun of(name: String, dependencies: List<Dependency>): KotlinSourceSet {
      return KotlinSourceSet(name, Dependencies(dependencies.toMutableList()))
    }

    @JvmStatic
    public fun of(name: String, vararg dependencies: Dependency): KotlinSourceSet {
      return KotlinSourceSet(name, Dependencies(*dependencies))
    }
  }
}
