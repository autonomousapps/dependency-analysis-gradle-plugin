package com.autonomousapps.kit

import com.autonomousapps.kit.render.Scribe

class BuildScript(
  val buildscript: BuildscriptBlock? = null,
  val plugins: Plugins = Plugins.EMPTY,
  val repositories: Repositories = Repositories.EMPTY,
  val android: AndroidBlock? = null,
  val sourceSets: SourceSets = SourceSets.EMPTY,
  val dependencies: Dependencies = Dependencies.EMPTY,
  val java: Java? = null,
  val kotlin: Kotlin? = null,
  val additions: String = "",
) {

  class Builder {
    var buildscript: BuildscriptBlock? = null
    var plugins: MutableList<Plugin> = mutableListOf()
    var repositories: List<Repository> = emptyList()
    var android: AndroidBlock? = null
    var sourceSets: List<String> = emptyList()
    var dependencies: List<Dependency> = emptyList()
    var java: Java? = null
    var kotlin: Kotlin? = null
    var additions: String = ""

    fun build(): BuildScript {
      return BuildScript(
        buildscript,
        Plugins(plugins),
        Repositories(repositories),
        android,
        SourceSets.ofNames(sourceSets),
        Dependencies(dependencies),
        java,
        kotlin,
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

    // A feature variant is always a 'sourceSet' declaration AND a registerFeature
    val featureVariantNames = java?.features?.map { it.sourceSetName }.orEmpty()
    val allSourceSets = SourceSets.ofNames(featureVariantNames) + sourceSets
    if (!allSourceSets.isEmpty()) {
      appendLine(scribe.use { s -> allSourceSets.render(s) })
    }

    java?.let { j -> appendLine(scribe.use { s -> j.render(s) }) }

    kotlin?.let { k -> appendLine(scribe.use { s -> k.render(s) }) }

    if (additions.isNotBlank()) {
      appendLine(additions)
    }

    if (!dependencies.isEmpty) {
      append(scribe.use { s -> dependencies.render(s) })
    }
  }
}
