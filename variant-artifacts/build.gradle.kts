// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.kotlin")
}

version = "0.1"

kotlin {
  explicitApi()
}

dagp {
  version(version)
  pom {
    name.set("Variant Artifacts support")
    description.set("A variant-artifacts support library for Gradle plugins")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2025")
  }
}

dependencies {
  api(gradleApi())

  // TODO: update testkit to support "gradle libraries" and add a functionTest source set.
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.junit.engine)
}
