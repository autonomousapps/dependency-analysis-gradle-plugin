package com.autonomousapps.kit

class RootProject(
  variant: String,
  val gradleProperties: GradleProperties = GradleProperties.DEFAULT,
  val settingsScript: SettingsScript = SettingsScript(),
  buildScript: BuildScript = BuildScript(),
  sources: List<Source> = emptyList()
) : Subproject(":", buildScript, sources, variant) {

  class Builder {
    var gradleProperties = GradleProperties.DEFAULT
    var settingsScript = SettingsScript()
    var buildScript = BuildScript()
    var sources = listOf<Source>()
    var variant: String? = null

    fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      buildScript = with(defaultBuildScriptBuilder()) {
        block(this)
        build()
      }
    }

    private fun defaultBuildScriptBuilder(): BuildScript.Builder {
      return BuildScript.Builder().apply {
        plugins = listOf(Plugin.dependencyAnalysisPlugin, Plugin.kotlinPlugin(apply = false))
        repositories = Repository.DEFAULT
        android = null
        dependencies = emptyList()
        additions = ""
      }
    }

    fun build(): RootProject {
      val variant = variant ?: error("'variant' must not be null")
      return RootProject(
        variant,
        gradleProperties,
        settingsScript,
        buildScript,
        sources
      )
    }
  }
}
