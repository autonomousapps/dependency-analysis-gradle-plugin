// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class JvmToolchain @JvmOverloads constructor(
  private val target: Int = 8,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("jvmToolchain(")
    s.append(target)
    s.append(")")
  }

  public companion object {
    @JvmField
    public val DEFAULT: JvmToolchain = JvmToolchain(8)
  }
}
