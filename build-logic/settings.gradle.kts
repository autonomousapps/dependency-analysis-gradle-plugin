// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") { name = "SNAPSHOTs" }
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
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") { name = "SNAPSHOTs" }
  }
}

rootProject.name = "build-logic"

include(":convention")
