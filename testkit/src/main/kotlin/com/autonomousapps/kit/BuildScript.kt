package com.autonomousapps.kit

import com.autonomousapps.kit.render.Scribe

class BuildScript(
  val buildscript: BuildscriptBlock? = null,
  val plugins: Plugins = Plugins.EMPTY,
  val repositories: Repositories = Repositories.EMPTY,
  val android: AndroidBlock? = null,
  val sourceSets: List<String> = emptyList(),
  val featureVariants: List<String> = emptyList(),
  val dependencies: Dependencies = Dependencies.EMPTY,
  val additions: String = "",
) {

  class Builder {
    var buildscript: BuildscriptBlock? = null
    var plugins: MutableList<Plugin> = mutableListOf()
    var repositories: List<Repository> = emptyList()
    var android: AndroidBlock? = null
    var sourceSets: List<String> = emptyList()
    var featureVariants: List<String> = emptyList()
    var dependencies: List<Dependency> = emptyList()
    var additions: String = ""

    fun build(): BuildScript {
      return BuildScript(
        buildscript,
        Plugins(plugins),
        Repositories(repositories),
        android,
        sourceSets,
        featureVariants,
        Dependencies(dependencies),
        additions
      )
    }
  }

  // TODO: finish migrating other types to Scribable
  fun render(scribe: Scribe): String = buildString {
    buildscript?.let { bs ->
      appendLine(scribe.use { s -> bs.render(s) })
    }
    if (!plugins.isEmpty) {
      appendLine(scribe.use { s -> plugins.render(s) })
    }

    if (!repositories.isEmpty) {
      appendLine(scribe.use { s -> repositories.render(s) })
    }

    android?.let { a -> appendLine(a) }

    val sourceSets = blockFrom("sourceSets", sourceSets + featureVariants)
    if (sourceSets.isNotBlank()) {
      appendLine(sourceSets)
    }

    // A feature variant is always a 'sourceSet' declaration AND a registerFeature
    val featureVariantsBlock = blockFrom("java", featureVariants.map {
      "registerFeature('$it') { usingSourceSet(sourceSets.$it) }"
    })
    if (featureVariantsBlock.isNotBlank()) {
      appendLine(featureVariantsBlock)
    }

    if (additions.isNotBlank()) {
      appendLine(additions)
    }

    if (!dependencies.isEmpty) {
      append(scribe.use { s -> dependencies.render(s) })
    }
  }

  companion object {
    private fun blockFrom(blockName: String, list: List<*>): String {
      return if (list.isEmpty()) "" else "$blockName {\n  ${list.joinToString("\n  ")}\n}\n"
    }
  }
}
