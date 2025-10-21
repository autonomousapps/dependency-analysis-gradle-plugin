// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * ```
 * debug {
 *   signingConfig signingConfigs.debug
 * }
 * ```
 *
 * TODO(tsr): enhance this with more type information than just `content: String`. E.g. signingConfigs.
 */
public data class BuildType @JvmOverloads constructor(
  public override val name: String,
  public val content: String = "",
) : Element.Block {

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append(content)
    }
  }

  // TODO(tsr): when migrating from `content`, need to consider how to access the buildTypes in KDSL.
  private fun renderKotlin(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append(content)
    }
  }

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun of(name: String, content: String = ""): BuildType {
      return BuildType(name, content)
    }
  }
}
