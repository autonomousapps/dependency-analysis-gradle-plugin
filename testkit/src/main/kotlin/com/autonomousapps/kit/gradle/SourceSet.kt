package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

class SourceSet(
  val name: String
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append(name)
  }

  companion object {
    @JvmStatic
    fun ofName(name: String): SourceSet = SourceSet(name)
  }
}
