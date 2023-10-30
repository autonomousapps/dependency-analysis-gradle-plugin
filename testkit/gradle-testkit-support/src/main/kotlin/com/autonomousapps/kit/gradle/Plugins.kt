package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Plugins(
  private val plugins: List<Plugin> = emptyList(),
) : Element.Block {

  public constructor(vararg plugins: Plugin) : this(plugins.toList())

  override val name: String = "plugins"

  public val isEmpty: Boolean = plugins.isEmpty()

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    plugins.forEach { it.render(s) }
  }

  public companion object {
    @JvmField
    public val EMPTY: Plugins = Plugins()
  }
}
