// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinSourceSets(
  private val sourceSets: List<KotlinSourceSet>,
) : Element.Block {

  override val name: String = "sourceSets"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    sourceSets.forEach { sourceSet ->
      sourceSet.render(s)
    }
  }

  public companion object {
    @JvmStatic
    public fun of(sourceSets: List<KotlinSourceSet>): KotlinSourceSets {
      return KotlinSourceSets(sourceSets)
    }

    @JvmStatic
    public fun of(vararg sourceSets: KotlinSourceSet): KotlinSourceSets {
      return KotlinSourceSets(sourceSets.toList())
    }
  }
}
