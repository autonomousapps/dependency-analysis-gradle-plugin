package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Repositories @JvmOverloads constructor(
  private val repositories: List<Repository> = emptyList(),
) : Element.Block {

  public constructor(vararg repositories: Repository) : this(repositories.toList())

  public val isEmpty: Boolean = repositories.isEmpty()

  override val name: String = "repositories"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.forEach { it.render(s) }
  }

  public operator fun plus(other: Repositories): Repositories {
    return Repositories((repositories + other.repositories).distinct())
  }

  public operator fun plus(other: Iterable<Repository>): Repositories {
    return Repositories((repositories + other).distinct())
  }

  public companion object {
    @JvmField public val EMPTY: Repositories = Repositories(emptyList())
    @JvmField public val DEFAULT_DEPENDENCIES: Repositories = Repositories(Repository.DEFAULT)
    @JvmField public val DEFAULT_PLUGINS: Repositories = Repositories(Repository.DEFAULT_PLUGINS)
  }
}
