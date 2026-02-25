// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle.kotlin

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class CompilerJvmTarget(
  private val target: Int,
) : Element.Line {

  override fun render(scribe: Scribe): String = scribe.line { s ->
    s.append("jvmTarget.set(")
    s.append(jvmTargetFrom(target))
    s.append(")")
    s.appendLine()
  }

  private companion object {
    fun jvmTargetFrom(target: Int): String {
      val suffix = if (target < 8) {
        error("Expected target >= 8. Was '$target'.")
      } else if (target == 8) {
        "1_8"
      } else {
        "$target"
      }

      // We make no effort to assume a max value.
      return "org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_$suffix"
    }
  }
}
