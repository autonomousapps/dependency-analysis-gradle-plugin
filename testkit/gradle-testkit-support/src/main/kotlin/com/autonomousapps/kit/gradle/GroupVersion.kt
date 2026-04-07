// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

/**
 * Represents `Project.group` and `Project.version` in a build script. Omits either value if null. If both values are
 * null, renders an empty string.
 *
 * ```
 * group = "com.group"
 * version = "1.0"
 * ```
 */
public class GroupVersion(
  private val group: String? = null,
  private val version: String? = null,
) : Element.MultiLine {

  /**
   * Will return an empty string if both [group] and [version] are null. It is the responsibility of the caller to
   * handle this correctly.
   */
  override fun render(scribe: Scribe): String {
    if (group == null && version == null) return ""

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
