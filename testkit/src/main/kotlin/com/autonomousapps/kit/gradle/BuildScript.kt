package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.render.Scribe

/** A build script. That is, a `build.gradle` or `build.gradle.kts` file. */
public class BuildScript(
  public val buildscript: BuildscriptBlock? = null,
  public val plugins: Plugins = Plugins.EMPTY,
  public val repositories: Repositories = Repositories.EMPTY,
  public val android: AndroidBlock? = null,
  public val sourceSets: SourceSets = SourceSets.EMPTY,
  public val dependencies: Dependencies = Dependencies.EMPTY,
  public val java: Java? = null,
  public val kotlin: Kotlin? = null,
  public val additions: String = "",
) {

  public fun render(scribe: Scribe): String = buildString {
    buildscript?.let { bs ->
      appendLine(scribe.use { s -> bs.render(s) })
    }
    if (!plugins.isEmpty) {
      appendLine(scribe.use { s -> plugins.render(s) })
    }

    if (!repositories.isEmpty) {
      appendLine(scribe.use { s -> repositories.render(s) })
    }

    android?.let {
      appendLine(scribe.use { s -> it.render(s) })
    }

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

  public class Builder {
    public var buildscript: BuildscriptBlock? = null
    public var plugins: MutableList<Plugin> = mutableListOf()
    public var repositories: List<Repository> = emptyList()
    public var android: AndroidBlock? = null
    public var sourceSets: List<String> = emptyList()
    public var dependencies: List<Dependency> = emptyList()
    public var java: Java? = null
    public var kotlin: Kotlin? = null
    public var additions: String = ""

    public fun build(): BuildScript {
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
}
