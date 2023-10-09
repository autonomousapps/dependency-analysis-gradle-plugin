package com.autonomousapps.kit

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class Kotlin @JvmOverloads constructor(
  private val jvmToolchain: JvmToolchain = JvmToolchain.DEFAULT,
) : Element.Block {

  override val name: String = "kotlin"

  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    jvmToolchain.render(s)
  }

  companion object {
    @JvmField
    val DEFAULT = Kotlin()

    @JvmStatic
    fun ofTarget(target: Int): Kotlin = Kotlin(JvmToolchain(target))
  }
}
