package com.autonomousapps.kit

open class Subproject(
  val name: String,
  val buildScript: BuildScript,
  val sources: List<Source>,
  val variant: String
) {

  class Builder {
    var name: String? = null
    var variant: String = "main"
    var buildScript: BuildScript = BuildScript()
    var sources: List<Source> = emptyList()

    fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      buildScript = with(defaultBuildScriptBuilder()) {
        block(this)
        build()
      }
    }

    private fun defaultBuildScriptBuilder(): BuildScript.Builder {
      return BuildScript.Builder().apply {
        plugins = emptyList()
        repositories = Repository.DEFAULT
        android = null
        dependencies = emptyList()
        additions = ""
      }
    }

    fun build(): Subproject {
      val name = name ?: error("'name' must not be null")
      return Subproject(
        name = name,
        buildScript = buildScript,
        sources = sources,
        variant = variant
      )
    }
  }
}
