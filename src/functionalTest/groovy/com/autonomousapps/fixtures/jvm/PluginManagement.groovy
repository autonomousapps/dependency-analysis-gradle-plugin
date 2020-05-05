package com.autonomousapps.fixtures.jvm

final class PluginManagement {

  final String block

  PluginManagement(String block = DEFAULT_PLUGIN_MANAGEMENT) {
    this.block = block
  }

  private static DEFAULT_PLUGIN_MANAGEMENT = """\
    pluginManagement {
      repositories {
        mavenLocal()
        gradlePluginPortal()
        jcenter()
        google()
      }
    }
  """.stripIndent()
}
