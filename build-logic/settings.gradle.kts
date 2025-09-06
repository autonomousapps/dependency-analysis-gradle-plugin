// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    gradlePluginPortal()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    google()
    mavenCentral()
    gradlePluginPortal() // gradle-publish-plugin
    //maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }
  }
}

rootProject.name = "build-logic"

include(":convention")
