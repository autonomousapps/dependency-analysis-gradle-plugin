// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinJvmTarget @JvmOverloads constructor(
  private val compilations: KotlinCompilations? = null,
) : Element.Block {

  override val name: String = "jvm"

  override fun render(scribe: Scribe): String {
    return if (compilations == null) {
      scribe.line { s -> s.append("$name()") }
    } else {
      scribe.block(this) { s ->
        compilations.render(s)
      }
    }
  }

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun of(compilations: KotlinCompilations? = null): KotlinJvmTarget {
      return KotlinJvmTarget(compilations)
    }
  }
}
