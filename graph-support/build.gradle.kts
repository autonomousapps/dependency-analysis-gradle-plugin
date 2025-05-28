// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("convention")
  id("com.autonomousapps.testkit")
  id("com.autonomousapps.dependency-analysis")
}

version = "0.5"

kotlin {
  explicitApi()
}

dagp {
  version(version)
  pom {
    name.set("Graph Support Library")
    description.set("A graph support library for the JVM")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
}

dependencies {
  api(libs.guava) {
    because("Graphs")
  }

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.moshi.core)
  testImplementation(libs.moshi.kotlin)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.junit.engine)
}
