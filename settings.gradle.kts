@file:Suppress("PropertyName", "UnstableApiUsage")

pluginManagement {
  includeBuild("build-logic")

  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.gradle.enterprise") version "3.10.2"
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
//    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
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
