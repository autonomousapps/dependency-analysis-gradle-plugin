package com.autonomousapps.jvm

final class Subproject {

  final String name
  final BuildScript buildScript
  final List<Source> sources

  Subproject(String name, BuildScript buildScript, List<Source> sources) {
    this.name = name
    this.buildScript = buildScript
    this.sources = sources
  }
}
