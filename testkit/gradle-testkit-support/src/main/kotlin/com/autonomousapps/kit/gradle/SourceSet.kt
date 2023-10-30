package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class SourceSet(
  public val name: String
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append(name)
  }

  public companion object {
    @JvmStatic
    public fun ofName(name: String): SourceSet = SourceSet(name)
  }
}
