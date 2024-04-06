// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "dependency-analysis-gradle-plugin"

pluginManagement {
  includeBuild("build-logic")
  includeBuild("testkit")

  // For dogfooding
  @Suppress("UNUSED_VARIABLE")
  val latestSnapshot = providers.gradleProperty("VERSION").get()

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
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.enterprise") version "3.15.1"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.20"
  }
}

plugins {
  id("com.gradle.enterprise")
}

// Yes, this is also in pluginManagement above. This is required for normal dependencies.
includeBuild("testkit")
// Address subprojects of this build (e.g. 'relocated.asm') by their coordinates.
// https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions
includeBuild(".")

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
}

val VERSION: String by extra.properties

gradleEnterprise {
  buildScan {
    publishAlways()
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
    tag(VERSION)
  }
}

include(":graph-support")

includeShadowed("antlr")
includeShadowed("asm-relocated")

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

fun includeShadowed(path: String) {
  include(":$path")
  project(":$path").projectDir = file("shadowed/$path")
}
