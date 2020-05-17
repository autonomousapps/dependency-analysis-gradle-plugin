package com.autonomousapps.kit

final class SettingsScript {

  final List<String> subprojects
  final PluginManagement pluginManagement
  final String rootProjectName

  SettingsScript(
    List<Subproject> subprojects = [],
    PluginManagement pluginManagement = new PluginManagement(),
    String rootProjectName = 'the-project'
  ) {
    this.rootProjectName = rootProjectName
    this.subprojects = subprojects.collect { it.name }
    this.pluginManagement = pluginManagement
  }

  @Override
  String toString() {
    return pluginManagement.block + '\n' +
      "rootProject.name = '$rootProjectName'\n" +
      subprojects.collect { "include ':$it'" }.join("\n")
  }
}
