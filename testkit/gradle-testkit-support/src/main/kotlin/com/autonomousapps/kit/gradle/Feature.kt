// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Feature @JvmOverloads constructor(
  private val featureName: String,
  public val sourceSetName: String = featureName,
) : Element.Block {

  override val name: String = "registerFeature(\"$featureName\")"

  override fun render(scribe: Scribe): String = when (scribe.dslKind) {
    DslKind.GROOVY -> renderGroovy(scribe)
    DslKind.KOTLIN -> renderKotlin(scribe)
  }

  private fun renderGroovy(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      s.append("usingSourceSet(")
      s.append("sourceSets.")
      s.append(sourceSetName)
      s.append(")")
    }
  }

  private fun renderKotlin(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      s.append("usingSourceSet(")
      s.append("sourceSets[")
      s.appendQuoted(sourceSetName)
      s.append("]")
      s.append(")")
    }
  }

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun ofName(
      featureName: String,
      sourceSetName: String = featureName,
    ): Feature = Feature(featureName, sourceSetName)
  }
}
