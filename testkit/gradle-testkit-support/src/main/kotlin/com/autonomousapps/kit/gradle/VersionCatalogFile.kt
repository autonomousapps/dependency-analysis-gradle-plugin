// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import org.intellij.lang.annotations.Language

/**
 * Represents a
 * [version catalog file](https://docs.gradle.org/current/userguide/platforms.html#sub::toml-dependencies-format).
 */
public class VersionCatalogFile(
  @Language("toml") public var content: String,
) {

  public companion object {
    public const val DEFAULT_PATH: String = "gradle/libs.versions.toml"
  }

  override fun toString(): String = content

  public class Builder {
    public var versions: MutableList<String> = mutableListOf()
    public var libraries: MutableList<String> = mutableListOf()
    public var bundles: MutableList<String> = mutableListOf()
    public var plugins: MutableList<String> = mutableListOf()

    public fun build(): VersionCatalogFile {
      val content = buildString {
        var didWrite = maybeWriteBlock("versions", versions, false)
        didWrite = maybeWriteBlock("libraries", libraries, didWrite)
        didWrite = maybeWriteBlock("bundles", bundles, didWrite)
        maybeWriteBlock("plugins", plugins, didWrite)
      }

      return VersionCatalogFile(content)
    }

    private fun StringBuilder.maybeWriteBlock(
      name: String,
      section: List<String>,
      prependLine: Boolean,
    ): Boolean {
      if (section.isNotEmpty()) {
        if (prependLine) appendLine()

        appendLine("[$name]")
        section.forEach { appendLine(it) }
        return true
      }

      return false
    }
  }
}
