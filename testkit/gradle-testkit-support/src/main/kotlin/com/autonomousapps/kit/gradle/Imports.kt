package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Scribe

public class Imports(
  private val imports: MutableList<Import>,
) {

  public constructor(vararg imports: Import) : this(imports.toMutableList())

  init {
    require(imports.isNotEmpty()) { "Imports must not be empty" }
  }

  public fun render(scribe: Scribe): String {
    return scribe.line { s ->
      imports.forEach { it.render(s) }
    }
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
