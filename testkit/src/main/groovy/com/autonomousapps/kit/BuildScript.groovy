package com.autonomousapps.kit

final class BuildScript {

  final BuildscriptBlock buildscript
  final List<Plugin> plugins
  final List<Repository> repositories
  final AndroidBlock android
  final List<Dependency> dependencies
  final String variant
  final String additions

  BuildScript(
    BuildscriptBlock buildscript = null, List<Plugin> plugins = [], List<Repository> repositories = [],
    AndroidBlock android = null,
    List<Dependency> dependencies = [], String variant, String additions = ''
  ) {
    this.buildscript = buildscript
    this.plugins = plugins
    this.repositories = repositories
    this.android = android
    this.dependencies = dependencies
    this.additions = additions
    this.variant = variant
  }

  @Override
  String toString() {
    def buildscriptBlock = buildscript != null ? "${buildscript}\n" : ''
    def pluginsBlock = blockFrom('plugins', plugins)
    def reposBlock = blockFrom('repositories', repositories)
    def androidBlock = android != null ? "${android}\n" : ''
    def dependenciesBlock = blockFrom('dependencies', dependencies)

    String e = ''
    if (!additions.isEmpty()) {
      e = "\n$additions"
    }

    return buildscriptBlock + pluginsBlock + reposBlock + androidBlock + dependenciesBlock + e
  }

  private static String blockFrom(String blockName, List<?> list) {
    if (list.isEmpty()) return ""
    return "$blockName {\n  ${list.join("\n  ")}\n}\n"
  }
}
