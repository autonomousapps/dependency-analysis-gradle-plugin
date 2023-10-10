package com.autonomousapps.kit

import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.SettingsScript

public class RootProject(
  variant: String,
  public val gradleProperties: GradleProperties = GradleProperties.minimalJvmProperties(),
  public val settingsScript: SettingsScript = SettingsScript(),
  buildScript: BuildScript = BuildScript(),
  sources: List<Source> = emptyList(),
  files: List<File>
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
    public var sources: List<Source> = listOf()
    public var variant: String? = null
    public val files: MutableList<File> = mutableListOf()

    // sub-builders
    private var buildScriptBuilder: BuildScript.Builder? = null

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
        plugins = mutableListOf(Plugin.dependencyAnalysisPlugin, Plugin.kotlinPlugin(apply = false))
        android = null
        dependencies = emptyList()
        additions = ""
      }
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
