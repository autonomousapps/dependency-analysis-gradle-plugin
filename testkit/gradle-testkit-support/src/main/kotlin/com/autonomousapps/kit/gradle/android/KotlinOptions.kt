// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class KotlinOptions @JvmOverloads constructor(
  private val jvmTarget: String = "1.8",
) : Element.Block {

  override val name: String = "kotlinOptions"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append("jvmTarget = \"")
      it.append(jvmTarget)
      it.append("\"")
    }
  }

  public companion object {
    @JvmField
    public val DEFAULT: KotlinOptions = KotlinOptions()
  }
}
