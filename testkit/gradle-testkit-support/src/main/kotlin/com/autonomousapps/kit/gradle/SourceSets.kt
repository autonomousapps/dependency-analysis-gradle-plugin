// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class SourceSets @JvmOverloads constructor(
  public val sourceSets: MutableList<SourceSet> = mutableListOf(),
) : Element.Block {

  public fun isEmpty(): Boolean = sourceSets.isEmpty()

  override val name: String = "sourceSets"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    sourceSets.forEach { it.render(s) }
  }

  public operator fun plus(other: SourceSets): SourceSets {
    val newSourceSets = ArrayList(sourceSets).apply {
      addAll(other.sourceSets)
    }
    return SourceSets(newSourceSets)
  }

  public companion object {
    @JvmField
    public val EMPTY: SourceSets = SourceSets()

    @JvmStatic
    public fun ofNames(names: Iterable<String>): SourceSets {
      return SourceSets(names.mapTo(mutableListOf()) { SourceSet(it) })
    }

    @JvmStatic
    public fun ofNames(vararg names: String): SourceSets = ofNames(names.toList())
  }
}
