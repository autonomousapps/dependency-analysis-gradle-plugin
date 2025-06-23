// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "testkit"

pluginManagement {
  includeBuild("../build-logic")

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

plugins {
  id("com.gradle.develocity") version "4.0.2"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
    google()
    mavenCentral()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

develocity {
  buildScan {
    publishing.onlyIf { true }
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
  }
}

include(":gradle-testkit-plugin")
include(":gradle-testkit-support")
include(":gradle-testkit-truth")
