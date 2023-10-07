package com.autonomousapps.kit

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Dependencies @JvmOverloads constructor(
  private val dependencies: List<Dependency> = emptyList(),
) : Element.Block {

  constructor(vararg dependencies: Dependency) : this(dependencies.toList())

  val isEmpty = dependencies.isEmpty()

  override val name: String = "dependencies"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    dependencies.forEach { it.render(s) }
  }

  companion object {
    @JvmField
    val EMPTY = Dependencies(emptyList())
  }
}
