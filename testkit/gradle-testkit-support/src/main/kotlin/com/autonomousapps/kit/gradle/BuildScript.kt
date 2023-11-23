package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.gradle.android.AndroidBlock
import com.autonomousapps.kit.render.Scribe
import org.intellij.lang.annotations.Language

/** A build script. That is, a `build.gradle` or `build.gradle.kts` file. */
public class BuildScript(
  public val buildscript: BuildscriptBlock? = null,
  public val plugins: Plugins = Plugins.EMPTY,
  public val group: String? = null,
  public val version: String? = null,
  public val repositories: Repositories = Repositories.EMPTY,
  public val android: AndroidBlock? = null,
  public val sourceSets: SourceSets = SourceSets.EMPTY,
  public val dependencies: Dependencies = Dependencies.EMPTY,
  public val java: Java? = null,
  public val kotlin: Kotlin? = null,
  public val additions: String = "",
) {

  private val groupVersion = GroupVersion(group = group, version = version)

  public fun render(scribe: Scribe): String = buildString {
    buildscript?.let { bs ->
      appendLine(scribe.use { s -> bs.render(s) })
    }

    if (!plugins.isEmpty) {
      appendLine(scribe.use { s -> plugins.render(s) })
    }

    // These two should be grouped together for aesthetic reasons
    scribe.use { s ->
      // One or both might be null
      val text = groupVersion.render(s)
      if (text.isNotBlank()) appendLine(text)
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
    public var group: String? = null
    public var version: String? = null
    public var repositories: MutableList<Repository> = mutableListOf()
    public var android: AndroidBlock? = null
    public var sourceSets: MutableList<String> = mutableListOf()
    public var dependencies: MutableList<Dependency> = mutableListOf()
    public var java: Java? = null
    public var kotlin: Kotlin? = null
    public var additions: String = ""

    public fun withGroovy(@Language("Groovy") script: String) {
      additions = script.trimIndent()
    }

    public fun dependencies(vararg dependencies: Dependency) {
      this.dependencies = dependencies.toMutableList()
    }

    public fun dependencies(dependencies: Iterable<Dependency>) {
      this.dependencies = dependencies.toMutableList()
    }

    public fun plugins(vararg plugins: Plugin) {
      this.plugins = plugins.toMutableList()
    }

    public fun plugins(plugins: Iterable<Plugin>) {
      this.plugins = plugins.toMutableList()
    }

    public fun build(): BuildScript {
      return BuildScript(
        buildscript = buildscript,
        plugins = Plugins(plugins),
        group = group,
        version = version,
        repositories = Repositories(repositories),
        android = android,
        sourceSets = SourceSets.ofNames(sourceSets),
        dependencies = Dependencies(dependencies),
        java = java,
        kotlin = kotlin,
        additions = additions
      )
    }
  }
}
