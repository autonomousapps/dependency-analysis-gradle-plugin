package com.autonomousapps.fixtures.jvm

final class Plugin {

  static final KOTLIN_VERSION = '1.3.72'

  final String id, version
  final boolean apply

  Plugin(String id, String version = null, boolean apply = true) {
    this.id = id
    this.version = version
    this.apply = apply
  }

  static Plugin dependencyAnalysisPlugin() {
    return new Plugin(
      'com.autonomousapps.dependency-analysis',
      System.getProperty("com.autonomousapps.pluginversion")
    )
  }

  static Plugin kotlinPlugin(boolean apply = true, String version = KOTLIN_VERSION) {
    return new Plugin('org.jetbrains.kotlin.jvm', version, apply)
  }

  @Override
  String toString() {
    String s = "id '$id'"
    if (version) {
      s += " version '$version'"
    }
    if (!apply) {
      s += ' apply false'
    }
    return s
  }
}
