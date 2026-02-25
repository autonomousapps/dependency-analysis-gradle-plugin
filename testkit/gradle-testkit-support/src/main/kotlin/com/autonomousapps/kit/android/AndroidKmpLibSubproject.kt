// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.android

import com.autonomousapps.kit.File
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.BuildScript

/**
 * A project that has applied the 'org.jetbrains.kotlin.multiplatform' and 'com.android.kotlin.multiplatform.library'
 * plugins. And Android KMP library.
 *
 * @see <a href="https://developer.android.com/kotlin/multiplatform/plugin">Android KMP Library</a>
 */
public class AndroidKmpLibSubproject(
  name: String,
  buildScript: BuildScript,
  sources: List<Source>,
  files: List<File> = emptyList(),
) : Subproject(
  name = name,
  buildScript = buildScript,
  sources = sources,
  files = files,
) {

  public class Builder(private val name: String) {

    public var buildScript: BuildScript = BuildScript()
    public var sources: List<Source> = emptyList()
    public val files: MutableList<File> = mutableListOf()

    private var buildScriptBuilder: BuildScript.Builder? = null

    // TODO(tsr): consider what kind of defaults this might have, if any.
    public fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      val builder = buildScriptBuilder ?: BuildScript.Builder()
      buildScript = with(builder) {
        block(this)
        // store for later building-upon
        buildScriptBuilder = this
        build()
      }
    }

    public fun withFile(path: String, content: String) {
      withFile(File(path, content))
    }

    public fun withFile(file: File) {
      files.add(file)
    }

    public fun build(): AndroidKmpLibSubproject {
      return AndroidKmpLibSubproject(
        name = name,
        buildScript = buildScript,
        sources = sources,
        files = files,
      )
    }
  }
}
