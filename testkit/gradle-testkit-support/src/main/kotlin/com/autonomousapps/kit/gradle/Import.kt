package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Import(
  private val import: String,
) : Element.Line {

  override fun render(scribe: Scribe): String {
    return scribe.line { s ->
      s.append("import ")
      s.append(import)
    }
  }
}
