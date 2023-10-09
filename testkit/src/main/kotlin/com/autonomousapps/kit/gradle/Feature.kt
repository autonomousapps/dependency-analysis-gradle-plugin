package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Feature @JvmOverloads constructor(
  private val featureName: String,
  val sourceSetName: String = featureName,
) : Element.Block {

  override val name: String = "registerFeature(\"$featureName\")"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      s.append("usingSourceSet(")
      s.append("sourceSets.")
      s.append(sourceSetName)
      s.append(")")
    }
  }

  companion object {
    @JvmOverloads
    @JvmStatic
    fun ofName(
      featureName: String,
      sourceSetName: String = featureName,
    ): Feature = Feature(featureName, sourceSetName)
  }
}
