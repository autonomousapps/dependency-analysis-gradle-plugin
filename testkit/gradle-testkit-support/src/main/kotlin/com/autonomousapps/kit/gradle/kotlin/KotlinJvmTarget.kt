// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinJvmTarget @JvmOverloads constructor(
  private val artifactName: String? = null,
  private val compilations: KotlinCompilations? = null,
) : Element.Block {

  override val name: String = "jvm"

  override fun render(scribe: Scribe): String {
    // e.g., `jvm("desktop")`
    val artifactName = if (artifactName != null) "\"$artifactName\"" else ""

    return if (compilations == null) {
      scribe.line { s -> s.append("$name($artifactName)") }
    } else {
      // `jvm { ... }` or `jvm("desktop") { ... }`
      val target = if (artifactName.isEmpty()) {
        name
      } else {
        "$name($artifactName)"
      }

      scribe.block(target) { s ->
        compilations.render(s)
      }
    }
  }

  public companion object {
    @JvmStatic
    public fun default(): KotlinJvmTarget = KotlinJvmTarget()
  }
}
