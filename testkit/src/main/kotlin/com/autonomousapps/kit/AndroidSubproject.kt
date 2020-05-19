package com.autonomousapps.kit

class AndroidSubproject(
  name: String,
  variant: String,
  buildScript: BuildScript,
  sources: List<Source>,
  files: List<File> = emptyList(),
  val manifest: AndroidManifest = AndroidManifest.DEFAULT,
  val styles: AndroidStyleRes = AndroidStyleRes.DEFAULT,
  val colors: AndroidColorRes = AndroidColorRes.DEFAULT,
  val layouts: List<AndroidLayout> = emptyList()
) : Subproject(
  name = name,
  buildScript = buildScript,
  sources = sources,
  files = files,
  variant = variant
) {

  class Builder {
    var name: String? = null
    var variant: String = "debug"
    var buildScript: BuildScript = BuildScript()
    var sources: List<Source> = emptyList()
    var manifest: AndroidManifest = AndroidManifest.DEFAULT
    var styles: AndroidStyleRes = AndroidStyleRes.DEFAULT
    var colors: AndroidColorRes = AndroidColorRes.DEFAULT
    var layouts: List<AndroidLayout> = emptyList()
    val files: MutableList<File> = mutableListOf()

    fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      buildScript = with(defaultBuildScriptBuilder()) {
        block(this)
        build()
      }
    }

    private fun defaultBuildScriptBuilder(): BuildScript.Builder {
      return BuildScript.Builder().apply {
        plugins = listOf(Plugin.androidAppPlugin)
        repositories = Repository.DEFAULT
        android = AndroidBlock.defaultAndroidBlock(false)
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

    fun build(): AndroidSubproject {
      val name = name ?: error("'name' must not be null")
      return AndroidSubproject(
        name = name,
        variant = variant,
        buildScript = buildScript,
        sources = sources,
        manifest = manifest,
        styles = styles,
        colors = colors,
        layouts = layouts,
        files = files
      )
    }
  }
}
