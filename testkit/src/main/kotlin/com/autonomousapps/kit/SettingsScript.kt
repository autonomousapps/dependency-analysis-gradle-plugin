package com.autonomousapps.kit

class SettingsScript(
  val pluginManagement: PluginManagement = PluginManagement.DEFAULT,
  val rootProjectName: String = "the-project",
  val plugins: MutableList<Plugin> = mutableListOf(),
  val subprojects: Set<String> = emptySet(),
  var additions: String = ""
) {

  override fun toString(): String {
    return pluginManagement.block + '\n' +
      pluginsBlock() +
      "rootProject.name = '$rootProjectName'\n" +
      subprojects.joinToString("\n") { "include ':$it'" } + '\n' +
      additions
  }

  private fun pluginsBlock() = buildString {
    if (plugins.isNotEmpty()) {
      appendLine("plugins {")
      plugins.forEach {
        appendLine("  $it")
      }
      appendLine("}")
    }
  }
}
