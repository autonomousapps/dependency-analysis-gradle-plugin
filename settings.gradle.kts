@file:Suppress("PropertyName", "UnstableApiUsage")

pluginManagement {
  includeBuild("build-logic")

  // For dogfooding
  val latestSnapshot = providers.gradleProperty("VERSION").get()

  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }
    gradlePluginPortal()
    mavenCentral()
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
  }
  plugins {
    id("com.autonomousapps.dependency-analysis") version "1.10.0"//latestSnapshot
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.gradle.enterprise") version "3.10.2"
    id("com.gradle.plugin-publish") version "0.11.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
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
    google()
    mavenCentral()
    //maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
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
