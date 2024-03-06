// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.render.Scribe

public class SettingsScript @JvmOverloads constructor(
  public var pluginManagement: PluginManagement = PluginManagement.DEFAULT,
  public var dependencyResolutionManagement: DependencyResolutionManagement? = DependencyResolutionManagement.DEFAULT,
  public var rootProjectName: String = "the-project",
  public var plugins: Plugins = Plugins.EMPTY,
  public var subprojects: Set<String> = emptySet(),

  /** For random stuff, as-yet unmodeled. */
  public var additions: String = "",
) {

  public fun render(scribe: Scribe): String = buildString {
    appendLine(scribe.use { s -> pluginManagement.render(s) })

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
}
