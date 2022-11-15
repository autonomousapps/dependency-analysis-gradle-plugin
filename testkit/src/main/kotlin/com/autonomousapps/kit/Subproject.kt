package com.autonomousapps.kit

open class Subproject(
  val name: String,
  val includedBuild: String? = null,
  val buildScript: BuildScript,
  val sources: List<Source>,
  val files: List<File>,
  val variant: String
) {

  /**
   * We only care about the subproject's name for equality comparisons and hashing.
   */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Subproject) return false
    if (name != other.name) return false
    if (includedBuild != other.includedBuild) return false
    return true
  }

  /**
   * We only care about the subproject's name for equality comparisons and hashing.
   */
  override fun hashCode(): Int = name.hashCode()

  class Builder {
    var name: String? = null
    var includedBuild: String? = null
    var variant: String = "main"
    var buildScript: BuildScript = BuildScript()
    var sources: List<Source> = emptyList()
    val files: MutableList<File> = mutableListOf()

    fun withBuildScript(block: BuildScript.Builder.() -> Unit) {
      buildScript = with(defaultBuildScriptBuilder()) {
        block(this)
        build()
      }
    }

    private fun defaultBuildScriptBuilder(): BuildScript.Builder {
      return BuildScript.Builder().apply {
        plugins = mutableListOf()
        repositories = Repository.DEFAULT
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

    fun build(): Subproject {
      val name = name ?: error("'name' must not be null")
      return Subproject(
        name = name,
        includedBuild = includedBuild,
        buildScript = buildScript,
        sources = sources,
        files = files,
        variant = variant
      )
    }
  }
}
