package com.autonomousapps.kit

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class SourceSets @JvmOverloads constructor(
  val sourceSets: List<SourceSet> = emptyList(),
) : Element.Block {

  fun isEmpty(): Boolean = sourceSets.isEmpty()

  override val name: String = "sourceSets"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    sourceSets.forEach { it.render(s) }
  }

  operator fun plus(other: SourceSets): SourceSets = SourceSets(sourceSets + other.sourceSets)

  companion object {
    @JvmField
    val EMPTY: SourceSets = SourceSets()

    @JvmStatic
    fun ofNames(names: List<String>): SourceSets = SourceSets(names.map { SourceSet(it) })

    @JvmStatic
    fun ofNames(vararg names: String): SourceSets = ofNames(names.toList())
  }
}
