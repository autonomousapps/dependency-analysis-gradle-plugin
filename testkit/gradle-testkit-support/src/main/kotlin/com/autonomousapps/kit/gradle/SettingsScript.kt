package com.autonomousapps.kit.gradle

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

    appendLine("rootProject.name = '$rootProjectName'")
    appendLine()
    appendLine(subprojects.joinToString("\n") { "include ':$it'" })

    if (additions.isNotBlank()) {
      appendLine()
      appendLine(additions)
    }
  }
}
