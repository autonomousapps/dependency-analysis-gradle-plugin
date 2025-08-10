// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class SourceSet(
  public val name: String,
) : Element.Line {

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.line { s ->
    s.append(name)
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.line { s ->
    s.append("create(")
    s.appendQuoted(name)
    s.append(")")
  }

  public companion object {
    @JvmStatic
    public fun ofName(name: String): SourceSet = SourceSet(name)
  }
}
