package com.autonomousapps.jvm

final class BuildScript {

  final List<Plugin> plugins
  final List<Repository> repositories
  final List<Dependency> dependencies
  final String extras

  BuildScript(
    List<Plugin> plugins, List<Repository> repositories, List<Dependency> dependencies,
    String extras = ''
  ) {
    this.plugins = plugins
    this.repositories = repositories
    this.dependencies = dependencies
    this.extras = extras
  }

  @Override
  String toString() {
    def pluginsBlock = blockFrom('plugins', plugins)
    def reposBlock = blockFrom('repositories', repositories)
    def dependenciesBlock = blockFrom('dependencies', dependencies)

    String e = ''
    if (!extras.isEmpty()) {
      e = "\n$extras"
    }

    return pluginsBlock + reposBlock + dependenciesBlock + e
  }

  private static String blockFrom(String blockName, List<?> list) {
    if (list.isEmpty()) return ""
    return "$blockName {\n  ${list.join("\n  ")}\n}\n"
  }
}
