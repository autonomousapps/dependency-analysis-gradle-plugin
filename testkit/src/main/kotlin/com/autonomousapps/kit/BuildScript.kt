package com.autonomousapps.kit

class BuildScript(
  val buildscript: BuildscriptBlock? = null,
  val plugins: List<Plugin> = emptyList(),
  val repositories: List<Repository> = emptyList(),
  val android: AndroidBlock? = null,
  val dependencies: List<Dependency> = emptyList(),
  val additions: String = ""
) {

  class Builder {
    var buildscript: BuildscriptBlock? = null
    var plugins: List<Plugin> = emptyList()
    var repositories: List<Repository> = emptyList()
    var android: AndroidBlock? = null
    var dependencies: List<Dependency> = emptyList()
    var additions: String = ""

    fun build(): BuildScript {
      return BuildScript(
        buildscript,
        plugins,
        repositories,
        android,
        dependencies,
        additions
      )
    }
  }

  override fun toString(): String {
    val buildscriptBlock = if (buildscript != null) "${buildscript}\n" else ""
    val pluginsBlock = blockFrom("plugins", plugins)
    val reposBlock = blockFrom("repositories", repositories)
    val androidBlock = if (android != null) "${android}\n" else ""
    val dependenciesBlock = blockFrom("dependencies", dependencies)

    val add =
      if (additions.isNotEmpty()) {
        "\n$additions"
      } else {
        ""
      }

    return buildscriptBlock + pluginsBlock + reposBlock + androidBlock + dependenciesBlock + add
  }

  companion object {
    private fun blockFrom(blockName: String, list: List<*>): String {
      return if (list.isEmpty()) "" else "$blockName {\n  ${list.joinToString("\n  ")}\n}\n"
    }
  }
}