// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class TestFixturesOptions(
  private val enable: Boolean = false
) : Element.Block {

  override fun render(scribe: Scribe): String = scribe.block(this) {
    scribe.line {
      it.append("enable = $enable")
    }
  }

  override val name: String = "testFixtures"
}
