package com.autonomousapps.kit.gradle

import com.autonomousapps.kit.render.Scribe

class SettingsScript @JvmOverloads constructor(
  var pluginManagement: PluginManagement = PluginManagement.DEFAULT,
  var dependencyResolutionManagement: DependencyResolutionManagement? = DependencyResolutionManagement.DEFAULT,
  var rootProjectName: String = "the-project",
  var plugins: Plugins = Plugins.EMPTY,
  var subprojects: Set<String> = emptySet(),

  /** For random stuff, as-yet unmodeled. */
  var additions: String = "",
) {

  fun render(scribe: Scribe): String = buildString {
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
