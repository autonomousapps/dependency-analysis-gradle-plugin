// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Scribe

public class SettingsScript @JvmOverloads constructor(
  public var pluginManagement: PluginManagement = PluginManagement.DEFAULT,
  public var buildscript: BuildscriptBlock? = null,
  public var plugins: Plugins = Plugins.EMPTY,
  public var dependencyResolutionManagement: DependencyResolutionManagement? = DependencyResolutionManagement.DEFAULT,
  public var rootProjectName: String = "the-project",
  public var subprojects: Set<String> = emptySet(),

  /** For random stuff, as-yet unmodeled. */
  public var additions: String = "",
) {

  public fun render(scribe: Scribe): String = buildString {
    appendLine(scribe.use { s -> pluginManagement.render(s) })

    buildscript?.let { bs ->
      appendLine(scribe.use { s -> bs.render(s) })
    }

    if (!plugins.isEmpty) {
      appendLine(scribe.use { s -> plugins.render(s) })
    }

    dependencyResolutionManagement?.let { d ->
      appendLine(scribe.use { s -> d.render(s) })
    }

    appendLine(renderRootProjectName(scribe.dslKind))
    appendLine()
    appendLine(subprojects.joinToString("\n") { renderInclude(scribe.dslKind, it) })

    if (additions.isNotBlank()) {
      appendLine()
      appendLine(additions)
    }
  }

  private fun renderRootProjectName(dslKind: DslKind) = when (dslKind) {
    DslKind.GROOVY -> "rootProject.name = '$rootProjectName'"
    DslKind.KOTLIN -> "rootProject.name = \"$rootProjectName\""
  }

  private fun renderInclude(dslKind: DslKind, subproject: String) = when (dslKind) {
    DslKind.GROOVY -> "include ':$subproject'"
    DslKind.KOTLIN -> "include(\":$subproject\")"
  }

  public class Builder {
    public var pluginManagement: PluginManagement = PluginManagement.DEFAULT
    public var buildscript: BuildscriptBlock? = null
    public var plugins: Plugins = Plugins.EMPTY
    public var dependencyResolutionManagement: DependencyResolutionManagement? = DependencyResolutionManagement.DEFAULT
    public var rootProjectName: String = "the-project"
    public var subprojects: Set<String> = emptySet()

    /** For random stuff, as-yet unmodeled. */
    public var additions: String = ""

    public fun plugins(vararg plugins: Plugin) {
      this.plugins = Plugins(plugins.toMutableList())
    }

    public fun plugins(plugins: Iterable<Plugin>) {
      this.plugins = Plugins(plugins.toMutableList())
    }

    public fun build(): SettingsScript {
      return SettingsScript(
        pluginManagement = pluginManagement,
        buildscript = buildscript,
        plugins = plugins,
        dependencyResolutionManagement = dependencyResolutionManagement,
        rootProjectName = rootProjectName,
        subprojects = subprojects,
        additions = additions,
      )
    }
  }
}
