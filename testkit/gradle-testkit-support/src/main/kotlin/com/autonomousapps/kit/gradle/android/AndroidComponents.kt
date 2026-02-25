// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

// TODO(tsr): model more thoroughly?
public class AndroidComponents(
  private val content: String,
) : Element.Block {

  override val name: String = "androidComponents"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.append(content)
    s.appendLine()
  }

  public companion object {
    @JvmStatic
    public fun of(content: String): AndroidComponents = AndroidComponents(content)
  }
}
