@file:Suppress("PropertyName", "UnstableApiUsage")

pluginManagement {
  includeBuild("build-logic")

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
    id("com.autonomousapps.dependency-analysis") version "1.24.0"//latestSnapshot
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.gradle.enterprise") version "3.15.1"
    id("com.gradle.plugin-publish") version "1.1.0"
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.0"
  }
}

plugins {
  id("com.gradle.enterprise")
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

rootProject.name = "dependency-analysis-gradle-plugin"

include(":graph-support")
include(":testkit")
include(":testkit-truth")

// shadowed projects
includeShadowed("antlr")
includeShadowed("asm-relocated")

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")

fun includeShadowed(path: String) {
  include(":$path")
  project(":$path").projectDir = file("shadowed/$path")
}
