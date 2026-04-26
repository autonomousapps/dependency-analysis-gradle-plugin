// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Kotlin(
  private val compilerOptions: CompilerOptions,
) : Element.Block {

  override val name: String = "kotlin"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    compilerOptions.render(s)
  }

  public companion object {
    @JvmField
    public val DEFAULT: Kotlin = Kotlin(CompilerOptions.DEFAULT)
  }
}
