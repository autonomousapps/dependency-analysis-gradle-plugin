// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.kotlin")
}

version = "0.19-SNAPSHOT"

dagp {
  version(version)
  pom {
    name.set("TestKit")
    description.set("A DSL for building test fixtures with Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2023")
  }
}

dependencies {
  api(platform(libs.kotlin.bom))
  api(gradleTestKit())

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.junit.engine)
}
