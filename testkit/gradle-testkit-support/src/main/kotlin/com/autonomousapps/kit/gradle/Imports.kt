// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Imports(
  private val imports: MutableList<Import>,
) : Element.MultiLine {

  public constructor(vararg imports: Import) : this(imports.toMutableList())

  init {
    require(imports.isNotEmpty()) { "Imports must not be empty" }
  }

  public override fun render(scribe: Scribe): String = scribe.line { s ->
    imports.forEach { i -> i.render(s) }
  }

  public companion object {
    @JvmStatic
    public fun of(vararg imports: String): Imports {
      return Imports(imports.mapTo(mutableListOf()) { Import(it) })
    }

    @JvmStatic
    public fun of(imports: Iterable<String>): Imports {
      return Imports(imports.mapTo(mutableListOf()) { Import(it) })
    }
  }
}
