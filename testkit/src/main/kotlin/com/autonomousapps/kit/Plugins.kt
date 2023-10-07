package com.autonomousapps.kit

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Plugins(
  private val plugins: List<Plugin> = emptyList(),
) : Element.Block {

  constructor(vararg plugins: Plugin) : this(plugins.toList())

  override val name: String = "plugins"

  val isEmpty = plugins.isEmpty()

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    plugins.forEach { it.render(s) }
  }

  companion object {
    @JvmField
    val EMPTY = Plugins()
  }
}
