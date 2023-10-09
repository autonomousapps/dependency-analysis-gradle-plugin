package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class JvmToolchain @JvmOverloads constructor(
  private val target: Int = 8,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("jvmToolchain(")
    s.append(target)
    s.append(")")
  }

  companion object {
    @JvmField
    val DEFAULT = JvmToolchain(8)
  }
}
