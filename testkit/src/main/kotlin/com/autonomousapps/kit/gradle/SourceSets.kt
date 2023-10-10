package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class SourceSets @JvmOverloads constructor(
  public val sourceSets: List<SourceSet> = emptyList(),
) : Element.Block {

  public fun isEmpty(): Boolean = sourceSets.isEmpty()

  override val name: String = "sourceSets"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    sourceSets.forEach { it.render(s) }
  }

  public operator fun plus(other: SourceSets): SourceSets = SourceSets(sourceSets + other.sourceSets)

  public companion object {
    @JvmField
    public val EMPTY: SourceSets = SourceSets()

    @JvmStatic
    public fun ofNames(names: List<String>): SourceSets = SourceSets(names.map { SourceSet(it) })

    @JvmStatic
    public fun ofNames(vararg names: String): SourceSets = ofNames(names.toList())
  }
}
