package com.autonomousapps.fixtures.jvm

final class BuildScript {

  final List<Plugin> plugins
  final List<Repository> repositories
  final List<Dependency> dependencies
  final String variant
  final String additions

  BuildScript(
    List<Plugin> plugins = [], List<Repository> repositories = [],
    List<Dependency> dependencies = [], String variant, String additions = ''
  ) {
    this.plugins = plugins
    this.repositories = repositories
    this.dependencies = dependencies
    this.additions = additions
    this.variant = variant
  }

  @Override
  String toString() {
    def pluginsBlock = blockFrom('plugins', plugins)
    def reposBlock = blockFrom('repositories', repositories)
    def dependenciesBlock = blockFrom('dependencies', dependencies)

    String e = ''
    if (!additions.isEmpty()) {
      e = "\n$additions"
    }

    return pluginsBlock + reposBlock + dependenciesBlock + e
  }

  private static String blockFrom(String blockName, List<?> list) {
    if (list.isEmpty()) return ""
    return "$blockName {\n  ${list.join("\n  ")}\n}\n"
  }
}
