// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinCompilations(
  private val content: String,
) : Element.Block {

  override val name: String = "compilations"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.append(content)
    s.appendLine()
  }
}
