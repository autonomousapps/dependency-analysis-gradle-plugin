// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("UnstableApiUsage")

plugins {
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  id("convention")
  id("org.jetbrains.dokka")
  alias(libs.plugins.dependencyAnalysis)
  id("com.autonomousapps.testkit")
}

version = "0.11-SNAPSHOT"
val isSnapshot: Boolean = version.toString().endsWith("SNAPSHOT")
val isRelease: Boolean = !isSnapshot

dagp {
  version(version)
  pom {
    name.set("Gradle TestKit Support Plugin")
    description.set("Make it less difficult to use Gradle TestKit to test your Gradle plugins")
    inceptionYear.set("2023")
  }
  publishTaskDescription(
    "Publishes plugin marker and plugin artifacts to Maven Central " +
      "(${if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "staging"})"
  )
}

gradlePlugin {
  plugins {
    create("plugin") {
      id = "com.autonomousapps.testkit"
      implementationClass = "com.autonomousapps.GradleTestKitPlugin"

      displayName = "Gradle TestKit Support Plugin (for plugins)"
      description = "Make it less difficult to use Gradle TestKit to test your Gradle plugins"
      tags.set(listOf("testing"))
    }
  }

  website.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
  vcsUrl.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
}

kotlin {
  explicitApi()
}

val dokkaJavadoc = tasks.named("dokkaJavadoc") {
  notCompatibleWithConfigurationCache("Uses 'project' at execution time")
}
// This task is added by Gradle when we use java.withJavadocJar()
tasks.named<Jar>("javadocJar") {
  from(dokkaJavadoc)
}

// This task fails and is a dependency of javadocJar (which doesn't fail), probably because there's no Java? Just
// disable it.
tasks.named("javadoc") {
  enabled = false
}

dependencies {
  api(platform(libs.kotlin.bom))
  api(gradleTestKit())

  dokkaHtmlPlugin(libs.kotlin.dokka)

  functionalTestImplementation(platform(libs.junit.bom))
  functionalTestImplementation(project(":gradle-testkit-support"))
  functionalTestImplementation(project(":gradle-testkit-truth"))
  // functionalTestImplementation(libs.junit.api)
  functionalTestImplementation(libs.junit.params)
  functionalTestImplementation(libs.truth)
  functionalTestRuntimeOnly(libs.junit.engine)
}

val check = tasks.named("check")

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check)
  }
}

val publishToPluginPortal = tasks.named("publishPlugins") {
  // Can't publish snapshots to the portal
  onlyIf { isRelease }
  shouldRunAfter(publishToMavenCentral)

  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(check)
  }
}

tasks.register("publishEverywhere") {
  dependsOn(publishToMavenCentral, publishToPluginPortal)

  group = "publishing"
  description = "Publishes to Plugin Portal and Maven Central"
}

dependencyAnalysis {
  issues {
    onAny {
      severity("fail")
    }
  }
}
