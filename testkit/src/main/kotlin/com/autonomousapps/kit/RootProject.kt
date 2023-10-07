package com.autonomousapps.kit

class RootProject(
  variant: String,
  val gradleProperties: GradleProperties = GradleProperties.minimalJvmProperties(),
  val settingsScript: SettingsScript = SettingsScript(),
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

  class Builder {
    var gradleProperties = GradleProperties.minimalJvmProperties()
    var settingsScript = SettingsScript()
    var buildScript = BuildScript()
    var sources = listOf<Source>()
    var variant: String? = null
    val files: MutableList<File> = mutableListOf()

    // sub-builders
    private var buildScriptBuilder: BuildScript.Builder? = null

    fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
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

    fun withFile(path: String, content: String) {
      withFile(File(path, content))
    }

    fun withFile(file: File) {
      files.add(file)
    }

    fun build(): RootProject {
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
