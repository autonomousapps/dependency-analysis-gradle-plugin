package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class PluginManagement(
  private val repositories: Repositories,
) : Element.Block {

  override val name: String = "pluginManagement"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.render(s)
  }

  public companion object {
    @JvmField
    public val DEFAULT: PluginManagement = PluginManagement(
      repositories = Repositories.DEFAULT_PLUGINS
    )
  }
}
