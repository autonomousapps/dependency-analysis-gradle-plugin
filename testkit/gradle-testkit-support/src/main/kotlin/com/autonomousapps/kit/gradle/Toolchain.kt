// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Element
import com.autonomousapps.kit.render.Scribe

public class Toolchain(
  public val javaLanguageVersion: Int,
) : Element.Block {

  override val name: String = "toolchain"

  /**
   * Example:
   * ```
   * java {
   *   toolchain {
   *     languageVersion.set(JavaLanguageVersion.of(17))
   *   }
   * }
   * ```
   */
  override fun render(scribe: Scribe): String = scribe.block(this) { s ->
    s.line {
      it.append("languageVersion.set(JavaLanguageVersion.of($javaLanguageVersion))")
    }
  }
}
