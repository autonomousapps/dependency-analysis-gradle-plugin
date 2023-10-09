package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Java @JvmOverloads constructor(
  val features: List<Feature> = emptyList(),
) : Element.Block {

  override val name: String = "java"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    features.forEach { it.render(s) }
  }

  companion object {
    @JvmStatic
    fun ofFeatures(features: List<Feature>): Java = Java(features)

    @JvmStatic
    fun ofFeatures(vararg features: Feature): Java = ofFeatures(features.toList())
  }
}
