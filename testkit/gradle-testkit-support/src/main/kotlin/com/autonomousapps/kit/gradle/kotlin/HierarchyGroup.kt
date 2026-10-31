// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class HierarchyGroup(
  public val group: String,
  public val targets: List<HierarchyTarget>,
) : Element.Block {

  override val name: String = "group"

  override fun render(scribe: Scribe): String = scribe.block("$name(\"$group\")") { s ->
    targets.forEach { it.render(s) }
  }

  public class Builder(private val group: String) {
    private val targets = mutableListOf<String>()

    public fun withAndroid() {
      targets.add("withAndroid()")
    }

    public fun withJvm() {
      targets.add("withJvm()")
    }

    public fun build(): HierarchyGroup {
      return HierarchyGroup(
        group = group,
        targets = targets.map { HierarchyTarget(it) },
      )
    }
  }
}
