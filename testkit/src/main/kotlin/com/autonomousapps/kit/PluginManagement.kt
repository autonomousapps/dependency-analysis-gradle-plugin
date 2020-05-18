package com.autonomousapps.kit

class PluginManagement(
  val block: String = ""
) {

  companion object {
    val DEFAULT = PluginManagement("""
      pluginManagement {
        repositories {
          mavenLocal()
          gradlePluginPortal()
          jcenter()
          google()
        }
      }
    """.trimIndent()
    )
  }
}
