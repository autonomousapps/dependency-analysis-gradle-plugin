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
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
  }
  plugins {
    id("com.autonomousapps.testkit") version "0.8"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.gradle.develocity") version "3.18.2"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
  }
}

plugins {
  id("com.gradle.develocity")
  id("org.gradle.toolchains.foojay-resolver-convention")
}

dependencyResolutionManagement {
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
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
