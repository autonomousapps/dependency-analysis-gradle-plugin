package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class GroupVersion(
  private val group: String? = null,
  private val version: String? = null,
) : Element.Line {

  /**
   * Will return an empty string if both [group] and [version] are null. It is the responsibility of the caller to
   * handle this correctly.
   */
  override fun render(scribe: Scribe): String {
    if (group == null && version == null) return ""

    // TODO I feel like this is wanting an Element.MultiLine or something.
    var addLine = false
    return scribe.line { s ->
      group?.let { g ->
        addLine = true
        s.append("group = ")
        s.appendQuoted(g)
      }
      version?.let { v ->
        if (addLine) s.appendLine()

        s.append("version = ")
        s.appendQuoted(v)
      }
    }
  }
}
