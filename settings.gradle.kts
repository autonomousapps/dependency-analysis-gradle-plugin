@file:Suppress("PropertyName", "UnstableApiUsage")

pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.gradle.enterprise") version "3.8.1"
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
    id("org.jetbrains.dokka") version "1.5.31"
  }
}

plugins {
  id("com.gradle.enterprise")
}

dependencyResolutionManagement {
  repositories {
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

include(":antlr")
include(":testkit")
include(":testkit-truth")

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
// TODO from Gradle 7
// https://docs.gradle.org/current/userguide/platforms.html
//enableFeaturePreview("VERSION_CATALOGS")
