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

    // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
    maven { url = uri("https://repo.gradle.org/libs-releases") }
    // Adding libs-snapshots is necessary to consume nightly versions
    maven { url = uri("https://repo.gradle.org/libs-snapshots") }
  }
  plugins {
    id("com.autonomousapps.testkit") version "0.8"
    id("com.gradleup.shadow") version "8.3.0"
    id("com.gradle.develocity") version "3.18.2"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.dokka") version "1.9.20"
  }
}

buildscript {
  repositories {
    gradlePluginPortal()
    // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
    maven { url = uri("https://repo.gradle.org/libs-releases") }
    // Adding libs-snapshots is necessary to consume nightly versions
    maven { url = uri("https://repo.gradle.org/libs-snapshots") }
  }

  dependencies {
    classpath("org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin:0.8.0")
    // classpath("com.gradle.develocity:com.gradle.develocity.gradle.plugin:3.19.2")
    // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
    classpath("org.gradle.experimental:gradle-public-api:8.13-rc-2")
  }
}

// plugins {
//   id("com.gradle.develocity")
// }

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

// develocity {
//   buildScan {
//     publishing.onlyIf { true }
//     termsOfUseUrl = "https://gradle.com/terms-of-service"
//     termsOfUseAgree = "yes"
//
//     tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
//   }
// }

include(":gradle-testkit-plugin")
include(":gradle-testkit-support")
include(":gradle-testkit-truth")
