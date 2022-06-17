@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal() // gradle-publish-plugin
//    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
  }
}

rootProject.name = "build-logic"

include(":convention")
