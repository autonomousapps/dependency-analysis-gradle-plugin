// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class VersionCatalog(
  private val catalogName: String,
  private val fromFiles: String,
) : Element.Block {

  override val name: String = catalogName

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    GradleProject.DslKind.GROOVY -> renderGroovy(scribe)
    GradleProject.DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append("from(files(\"")
      it.append(fromFiles)
      it.append("\"))")
    }
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.block("create(\"$catalogName\")") { s ->
    s.line {
      it.append("from(files(\"")
      it.append(fromFiles)
      it.append("\"))")
    }
  }
}
