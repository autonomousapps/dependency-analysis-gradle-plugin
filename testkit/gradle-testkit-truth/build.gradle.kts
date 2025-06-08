// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("build-logic.lib.kotlin")
}

version = "1.7-SNAPSHOT"

dagp {
  version(version)
  pom {
    name.set("TestKit Truth")
    description.set("A Truth extension for Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
  }
}

kotlin {
  explicitApi()
}

dependencies {
  api(project(":gradle-testkit-support")) {
    because("Uses BuildArtifact")
  }
  api(kotlin("stdlib"))
  api(gradleTestKit())
  api(libs.truth)

  implementation(libs.errorProne) {
    because("Uses @CanIgnoreReturnValue")
  }
  implementation(libs.guava)

  dokkaHtmlPlugin(libs.kotlin.dokka)
}
