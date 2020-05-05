package com.autonomousapps.jvm

final class SettingScript {

  final List<String> subprojects
  final PluginManagement pluginManagement
  final String rootProjectName

  SettingScript(
    List<Subproject> subprojects = [],
    PluginManagement pluginManagement = new PluginManagement(),
    String rootProjectName = 'jvm-project'
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
