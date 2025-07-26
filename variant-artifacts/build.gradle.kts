// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.kotlin")
}

version = "0.2"

dagp {
  version(version)
  pom {
    name.set("Variant Artifacts support")
    description.set("A variant-artifacts support library for Gradle plugins")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2025")
  }
}

gradleTestKitSupport {
  registerFunctionalTest()
  withSupportLibrary()
  withTruthLibrary()
}

dependencies {
  api(gradleApi())

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.api)
  testImplementation(libs.junit.params)
  testImplementation(libs.truth)

  testRuntimeOnly(libs.junit.engine)

  "functionalTestImplementation"(platform(libs.junit.bom))
  "functionalTestImplementation"(libs.junit.api)
  "functionalTestImplementation"(libs.truth)

  "functionalTestImplementation"(project)
  "functionalTestRuntimeOnly"(libs.junit.engine)
}
