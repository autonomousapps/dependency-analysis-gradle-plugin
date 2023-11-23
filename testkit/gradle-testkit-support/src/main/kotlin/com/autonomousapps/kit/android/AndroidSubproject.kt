package com.autonomousapps.kit.android

import com.autonomousapps.kit.File
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.android.AndroidBlock

public class AndroidSubproject(
  name: String,
  variant: String,
  buildScript: BuildScript,
  sources: List<Source>,
  files: List<File> = emptyList(),
  public val manifest: AndroidManifest? = AndroidManifest.DEFAULT_APP,
  public val styles: AndroidStyleRes? = AndroidStyleRes.DEFAULT,
  public val strings: AndroidStringRes? = AndroidStringRes.DEFAULT,
  public val colors: AndroidColorRes? = AndroidColorRes.DEFAULT,
  public val layouts: List<AndroidLayout>? = null,
) : Subproject(
  name = name,
  buildScript = buildScript,
  sources = sources,
  files = files,
  variant = variant
) {

  public class Builder {
    public var name: String? = null
    public var variant: String = "debug"
    public var buildScript: BuildScript = BuildScript()
    public var sources: List<Source> = emptyList()
    public var manifest: AndroidManifest? = AndroidManifest.DEFAULT_APP
    public var styles: AndroidStyleRes? = AndroidStyleRes.DEFAULT
    public var strings: AndroidStringRes? = AndroidStringRes.DEFAULT
    public var colors: AndroidColorRes? = AndroidColorRes.DEFAULT
    public var layouts: List<AndroidLayout>? = null
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
        plugins = mutableListOf()
        android = AndroidBlock.defaultAndroidAppBlock(false)
        dependencies = mutableListOf()
        additions = ""
      }
    }

    public fun withFile(path: String, content: String) {
      withFile(File(path, content))
    }

    public fun withFile(file: File) {
      files.add(file)
    }

    public fun build(): AndroidSubproject {
      val name = name ?: error("'name' must not be null")
      return AndroidSubproject(
        name = name,
        variant = variant,
        buildScript = buildScript,
        sources = sources,
        manifest = manifest,
        styles = styles,
        strings = strings,
        colors = colors,
        layouts = layouts,
        files = files
      )
    }
  }
}
