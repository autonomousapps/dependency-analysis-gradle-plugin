package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class DependencyResolutionManagement(
  private val repositories: Repositories,
) : Element.Block {

  override val name: String = "dependencyResolutionManagement"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.render(s)
  }

  companion object {
    @JvmField
    val DEFAULT = DependencyResolutionManagement(
      repositories = Repositories.DEFAULT_DEPENDENCIES,
    )
  }
}
