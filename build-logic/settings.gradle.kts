// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()

    // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
    maven { url = uri("https://repo.gradle.org/libs-releases") }
    // Adding libs-snapshots is necessary to consume nightly versions
    maven { url = uri("https://repo.gradle.org/libs-snapshots") }
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

    // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
    maven { url = uri("https://repo.gradle.org/libs-releases") }
    // Adding libs-snapshots is necessary to consume nightly versions
    maven { url = uri("https://repo.gradle.org/libs-snapshots") }
  }
}

rootProject.name = "build-logic"

include(":convention")
