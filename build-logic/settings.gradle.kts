// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention")
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
