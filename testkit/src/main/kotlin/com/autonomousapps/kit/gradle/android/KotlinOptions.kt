package com.autonomousapps.kit.gradle.android

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class KotlinOptions @JvmOverloads constructor(
  private val jvmTarget: String = "1.8"
) : Element.Block {

  override val name: String = "kotlinOptions"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append("jvmTarget = \"")
      it.append(jvmTarget)
      it.append("\"")
    }
  }

  companion object {
    @JvmField
    val DEFAULT = KotlinOptions()
  }
}
