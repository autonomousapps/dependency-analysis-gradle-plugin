// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.render

public sealed interface Element {

  public fun render(scribe: Scribe): String

  public fun start(indent: Int): String = " ".repeat(indent)

  public interface Block : Element {
    public val name: String
  }

  public interface Line : Element

  public interface MultiLine : Element
}
