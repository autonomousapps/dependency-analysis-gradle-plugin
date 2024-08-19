// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit

import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.SettingsScript
import com.autonomousapps.kit.gradle.VersionCatalogFile
import org.intellij.lang.annotations.Language

/**
 * Represents the root project of a Gradle build. Different from a [Subproject] in that it has a
 * [GradleProperties] and a [SettingsScript].
 */
public class RootProject(
  variant: String,
  public val gradleProperties: GradleProperties = GradleProperties.minimalJvmProperties(),
  public val settingsScript: SettingsScript = SettingsScript(),
  buildScript: BuildScript = BuildScript(),
  sources: List<Source> = emptyList(),
  files: List<File>,
) : Subproject(
  name = ":",
  buildScript = buildScript,
  sources = sources,
  files = files,
  variant = variant
) {

  public class Builder {
    public var gradleProperties: GradleProperties = GradleProperties.minimalJvmProperties()
    public var settingsScript: SettingsScript = SettingsScript()
    public var buildScript: BuildScript = BuildScript()
    public var sources: List<Source> = emptyList()
    public var variant: String? = null
    public val files: MutableList<File> = mutableListOf()

    /*
     * sub-builders
     */

    private var settingsScriptBuilder: SettingsScript.Builder? = null
    private var buildScriptBuilder: BuildScript.Builder? = null

    public fun withSettingsScript(block: SettingsScript.Builder.() -> Unit) {
      val builder = settingsScriptBuilder ?: SettingsScript.Builder()
      settingsScript = with(builder) {
        block(this)
        // store for later building-upon
        settingsScriptBuilder = this
        build()
      }
    }

    public fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      val builder = buildScriptBuilder ?: defaultBuildScriptBuilder()
      buildScript = with(builder) {
        block(this)
        // store for later building-upon
        buildScriptBuilder = this
        build()
      }
    }

    private fun defaultBuildScriptBuilder(): BuildScript.Builder {
      return BuildScript.Builder().apply {
        plugins = mutableListOf()
        android = null
        dependencies = mutableListOf()
        additions = ""
      }
    }

    /**
     * Add a [version catalog](https://docs.gradle.org/current/userguide/platforms.html) at the
     * [default path][VersionCatalogFile.DEFAULT_PATH], with [content].
     */
    public fun withVersionCatalog(@Language("toml") content: String) {
      withVersionCatalog(
        path = VersionCatalogFile.DEFAULT_PATH,
        content = content
      )
    }

    /**
     * Add a [version catalog](https://docs.gradle.org/current/userguide/platforms.html) at [path], with [content].
     */
    public fun withVersionCatalog(
      path: String,
      @Language("toml") content: String,
    ) {
      withVersionCatalog(File(path, content))
    }

    /**
     * Add a [version catalog](https://docs.gradle.org/current/userguide/platforms.html) with path and content specified
     * by [file].
     */
    public fun withVersionCatalog(file: File) {
      withFile(file)
    }

    public fun withFile(path: String, content: String) {
      withFile(File(path, content))
    }

    public fun withFile(file: File) {
      files.add(file)
    }

    public fun build(): RootProject {
      val variant = variant ?: error("'variant' must not be null")
      return RootProject(
        variant = variant,
        gradleProperties = gradleProperties,
        settingsScript = settingsScript,
        buildScript = buildScript,
        sources = sources,
        files = files
      )
    }
  }
}
