// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class HierarchyRoot(
  public val targets: List<HierarchyTarget>,
  public val groups: List<HierarchyGroup>,
) : Element.Block {

  override val name: String = "common"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    targets.forEach { it.render(s) }
    groups.forEach { it.render(s) }
  }

  public class Builder {
    private val targets = mutableListOf<String>()
    private val groups = mutableMapOf<String, HierarchyGroup.Builder>()

    public fun withAndroid() {
      targets.add("withAndroid()")
    }

    public fun withJvm() {
      targets.add("withJvm()")
    }

    public fun group(group: String, block: (HierarchyGroup.Builder) -> Unit) {
      val builder = groups.computeIfAbsent(group) { HierarchyGroup.Builder(group) }
      block(builder)
    }

    public fun build(): HierarchyRoot {
      return HierarchyRoot(
        targets = targets.map { HierarchyTarget(it) },
        groups = groups.values.map { it.build() },
      )
    }
  }
}
