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
          mavenCentral()
          google()
          //maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
        }
      }
    """.trimIndent()
    )
  }
}
