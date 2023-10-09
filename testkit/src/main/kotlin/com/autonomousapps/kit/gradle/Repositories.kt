package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Repositories @JvmOverloads constructor(
  private val repositories: List<Repository> = emptyList(),
) : Element.Block {

  constructor(vararg repositories: Repository) : this(repositories.toList())

  val isEmpty = repositories.isEmpty()

  override val name: String = "repositories"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.forEach { it.render(s) }
  }

  companion object {
    @JvmField
    val EMPTY = Repositories(emptyList())

    @JvmField
    val DEFAULT_DEPENDENCIES = Repositories(Repository.DEFAULT)

    @JvmField
    val DEFAULT_PLUGINS = Repositories(Repository.DEFAULT_PLUGINS)
  }
}
