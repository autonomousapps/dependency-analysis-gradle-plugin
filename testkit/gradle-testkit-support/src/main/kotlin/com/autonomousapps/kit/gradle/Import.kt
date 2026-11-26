// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Import(
  private val import: String,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("import ")
    s.append(import)
  }

  public companion object {
    @JvmStatic
    public fun of(import: String): Import = Import(import)

    @JvmStatic
    public fun of(vararg imports: String): MutableList<Import> = imports.mapTo(mutableListOf()) { Import(it) }

    @JvmStatic
    public fun of(imports: Iterable<String>): MutableList<Import> = imports.mapTo(mutableListOf()) { Import(it) }
  }
}
