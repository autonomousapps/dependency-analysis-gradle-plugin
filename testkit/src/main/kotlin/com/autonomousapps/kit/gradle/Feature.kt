package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Feature @JvmOverloads constructor(
  private val featureName: String,
  public val sourceSetName: String = featureName,
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

  public companion object {
    @JvmOverloads
    @JvmStatic
    public fun ofName(
      featureName: String,
      sourceSetName: String = featureName,
    ): Feature = Feature(featureName, sourceSetName)
  }
}
