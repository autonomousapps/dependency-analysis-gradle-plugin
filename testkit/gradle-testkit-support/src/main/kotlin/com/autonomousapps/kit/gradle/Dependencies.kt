package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Dependencies @JvmOverloads constructor(
  private val dependencies: List<Dependency> = emptyList(),
) : Element.Block {

  public constructor(vararg dependencies: Dependency) : this(dependencies.toList())

  public val isEmpty: Boolean = dependencies.isEmpty()

  override val name: String = "dependencies"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    dependencies.forEach { it.render(s) }
  }

  public companion object {
    @JvmField
    public val EMPTY: Dependencies = Dependencies(emptyList())
  }
}
