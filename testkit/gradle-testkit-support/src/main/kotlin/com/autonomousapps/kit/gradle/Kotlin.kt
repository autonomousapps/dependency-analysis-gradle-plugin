// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Kotlin @JvmOverloads constructor(
  private val jvmToolchain: JvmToolchain = JvmToolchain.DEFAULT,
) : Element.Block {

  override val name: String = "kotlin"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    jvmToolchain.render(s)
  }

  public companion object {
    @JvmField
    public val DEFAULT: Kotlin = Kotlin()

    @JvmStatic
    public fun ofTarget(target: Int): Kotlin = Kotlin(JvmToolchain(target))
  }
}
