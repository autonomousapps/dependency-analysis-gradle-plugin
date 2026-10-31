// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class HierarchyTarget(
  public val target: String
) : Element.Line {
  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append(target)
  }
}
