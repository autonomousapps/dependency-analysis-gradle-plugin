// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit

import com.autonomousapps.kit.gradle.BuildScript

/**
 * A Gradle project (module) consists of:
 *
 * 1. A [name] (path)
 * 2. [Build script][buildScript]
 * 3. (Optionally) [sources]
 * 4. (Optionally) arbitrary [files]
 * 5. A JVM [source set][variant] or Android [variant](https://developer.android.com/build/build-variants) name.
 *    Defaults to "main".
 */
public open class Subproject(
  public val name: String,
  public val includedBuild: String? = null,
  public val buildScript: BuildScript,
  public val sources: List<Source>,
  public val files: List<File>,
  public val variant: String,
) {

  /** We only care about the subproject's name for equality comparisons and hashing. */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Subproject) return false
    if (name != other.name) return false
    if (includedBuild != other.includedBuild) return false
    return true
  }

  /** We only care about the subproject's name for equality comparisons and hashing. */
  override fun hashCode(): Int = name.hashCode()

  public class Builder {
    public var name: String? = null
    public var includedBuild: String? = null
    public var variant: String = "main"
    public var sources: MutableList<Source> = mutableListOf()
    public var files: MutableList<File> = mutableListOf()

    public var buildScript: BuildScript? = null
    private val buildScriptBuilder: BuildScript.Builder = BuildScript.Builder()

    public fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      buildScript = with(buildScriptBuilder) {
        block(this)
        build()
      }
    }

    public fun withFile(path: String, content: String) {
      withFile(File(path, content))
    }

    public fun withFile(file: File) {
      files.add(file)
    }

    public fun build(): Subproject {
      val name = name ?: error("'name' must not be null")
      return Subproject(
        name = name,
        includedBuild = includedBuild,
        buildScript = buildScript ?: buildScriptBuilder.build(),
        sources = sources,
        files = files,
        variant = variant
      )
    }
  }
}
