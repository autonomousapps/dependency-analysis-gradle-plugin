package com.autonomousapps.kit

class Subproject {

  final String name
  final BuildScript buildScript
  final List<Source> sources
  final String variant

  Subproject(String name, BuildScript buildScript, List<Source> sources) {
    this.name = name
    this.buildScript = buildScript
    this.sources = sources
    this.variant = buildScript.variant
  }
}
