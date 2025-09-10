// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.kotlin")
}

version = "0.3.1"

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
  testRuntimeOnly(libs.junit.launcher)

  "functionalTestImplementation"(platform(libs.junit.bom))
  "functionalTestImplementation"(project)
  "functionalTestImplementation"(libs.junit.api)
  "functionalTestImplementation"(libs.truth)

  "functionalTestRuntimeOnly"(libs.junit.engine)
  "functionalTestRuntimeOnly"(libs.junit.launcher)
}
