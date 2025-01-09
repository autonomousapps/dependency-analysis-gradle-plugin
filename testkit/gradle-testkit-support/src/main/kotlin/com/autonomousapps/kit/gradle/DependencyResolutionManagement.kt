// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class DependencyResolutionManagement(
  private val repositories: Repositories,
) : Element.Block {

  override val name: String = "dependencyResolutionManagement"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    repositories.render(s)
  }

  public companion object {
    @JvmField
    public val DEFAULT: DependencyResolutionManagement = DependencyResolutionManagement(
      repositories = Repositories.DEFAULT_DEPENDENCIES,
    )
  }
}
