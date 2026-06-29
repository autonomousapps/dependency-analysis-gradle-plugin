// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.android

public class AndroidLayout(
  public val filename: String,
  public val content: String,
) {

  override fun toString(): String = content

  public class Builder(private val name: String) {
    public fun withContent(content: String): AndroidLayout {
      return AndroidLayout(
        filename = name,
        content = content,
      )
    }
  }

  public companion object {
    /** The [name] should be just the filename. The path (e.g., `src/main/res/layout/<name>`) is assumed. */
    @JvmStatic
    public fun named(name: String): Builder = Builder(name)
  }
}
