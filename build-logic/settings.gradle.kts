// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }

    gradlePluginPortal()
    mavenCentral()

    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
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

    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
  }
}

rootProject.name = "build-logic"

include(":convention")
