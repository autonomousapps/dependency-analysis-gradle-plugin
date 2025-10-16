// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "dependency-analysis-gradle-plugin"

pluginManagement {
  includeBuild("build-logic")
  includeBuild("testkit")

  // For dogfooding
  @Suppress("unused")
  val latestSnapshot = providers.gradleProperty("VERSION").get()

  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    gradlePluginPortal()
    mavenCentral()

    // This is for the `com.android.tools.metalava:metalava` dependency
    exclusiveContent {
      forRepository {
        maven(url = "https://dl.google.com/dl/android/maven2/")
      }
      filter {
        includeGroup("com.android.tools.metalava")
        includeGroup("com.android.tools")
        includeGroup("com.android.tools.layoutlib")
        includeGroup("com.android.tools.ddms")
        includeGroup("com.android.tools.build")
        includeGroup("com.android.tools.analytics-library")
        includeGroup("com.android.tools.lint")
        includeGroup("com.android.tools.external.com-intellij")
        includeGroup("com.android.tools.external.org-jetbrains")
      }
    }

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
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
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

develocity {
  buildScan {
    val isCI = providers.environmentVariable("CI").isPresent
    val isEnabled = providers.gradleProperty("dependency.analysis.scans.publish").getOrElse("false").toBoolean()

    publishing.onlyIf { isCI || isEnabled }
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"

    tag(if (isCI) "CI" else "Local")
    tag(VERSION)
  }
}

include(":graph-support")
include(":variant-artifacts")

includeShadowed("antlr")
includeShadowed("asm-relocated")
includeShadowed("kotlin-editor-relocated")

fun includeShadowed(path: String) {
  include(":$path")
  project(":$path").projectDir = file("shadowed/$path")
}
