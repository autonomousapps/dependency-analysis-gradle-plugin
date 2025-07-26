// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.plugin")
}

version = "0.14-SNAPSHOT"
val isSnapshot: Boolean = version.toString().endsWith("SNAPSHOT")
val isRelease: Boolean = !isSnapshot

dagp {
  version(version)
  pom {
    name.set("Gradle TestKit Support Plugin")
    description.set("Make it less difficult to use Gradle TestKit to test your Gradle plugins")
    inceptionYear.set("2023")
  }
}

gradlePlugin {
  plugins {
    create("plugin") {
      id = "com.autonomousapps.testkit"
      implementationClass = "com.autonomousapps.GradleTestKitPlugin"

      displayName = "Gradle TestKit Support Plugin (for plugins)"
      description = "Make it less difficult to use Gradle TestKit to test your Gradle plugins"
      tags.set(setOf("testing"))
    }
  }

  website.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
  vcsUrl.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
}

dependencies {
  api(platform(libs.kotlin.bom))
  api(gradleTestKit())

  functionalTestImplementation(platform(libs.junit.bom))
  functionalTestImplementation(project(":gradle-testkit-support"))
  functionalTestImplementation(project(":gradle-testkit-truth"))
  functionalTestImplementation(libs.junit.params)
  functionalTestImplementation(libs.truth)
  functionalTestRuntimeOnly(libs.junit.engine)
}

val publishToMavenCentral = tasks.named("publishToMavenCentral") {
  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(tasks.check)
  }
}

val publishToPluginPortal = tasks.named("publishPlugins") {
  // Can't publish snapshots to the portal
  onlyIf { isRelease }
  shouldRunAfter(publishToMavenCentral)

  // Note that publishing a release requires a successful smokeTest
  if (isRelease) {
    dependsOn(tasks.check)
  }
}

tasks.register("publishEverywhere") {
  dependsOn(publishToMavenCentral, publishToPluginPortal)

  group = "publishing"
  description = "Publishes to Plugin Portal and Maven Central"
}
