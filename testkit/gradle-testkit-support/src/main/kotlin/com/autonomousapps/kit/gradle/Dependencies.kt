// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Dependencies @JvmOverloads constructor(
  private val dependencies: MutableList<Dependency> = mutableListOf(),
) : Element.Block {

  public constructor(vararg dependencies: Dependency) : this(dependencies.toMutableList())

  public val isEmpty: Boolean = dependencies.isEmpty()

  override val name: String = "dependencies"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    dependencies.forEach { it.render(s) }
  }

  public companion object {
    @JvmField
    public val EMPTY: Dependencies = Dependencies(mutableListOf())
  }
}
