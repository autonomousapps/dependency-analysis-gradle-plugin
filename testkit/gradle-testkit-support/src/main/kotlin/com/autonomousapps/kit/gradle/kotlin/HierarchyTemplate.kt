// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class HierarchyTemplate(
  public val root: HierarchyRoot,
) : Element.Block {

  override val name: String = "@OptIn(ExperimentalKotlinGradlePluginApi::class) applyDefaultHierarchyTemplate"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    root.render(s)
  }

  public class Builder {
    private var rootBuilder: HierarchyRoot.Builder? = null

    public fun common(block: (HierarchyRoot.Builder) -> Unit) {
      val rootBuilder = rootBuilder ?: HierarchyRoot.Builder()
      block(rootBuilder)
      this.rootBuilder = rootBuilder
    }

    public fun build(): HierarchyTemplate {
      val rootBuilder = requireNotNull(rootBuilder) { "rootBuilder was null" }
      return HierarchyTemplate(rootBuilder.build())
    }
  }
}
