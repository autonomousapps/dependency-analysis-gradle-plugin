// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("convention")
  id("org.jetbrains.dokka")
  alias(libs.plugins.dependencyAnalysis)
  id("com.autonomousapps.testkit")
}

version = "1.6.1"

dagp {
  version(version)
  pom {
    name.set("TestKit Truth")
    description.set("A Truth extension for Gradle TestKit")
    url.set("https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin")
    inceptionYear.set("2022")
  }
  publishTaskDescription("Publishes to Maven Central and promotes.")
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
  api(project(":gradle-testkit-support")) {
    because("Uses BuildArtifact")
  }
  api(kotlin("stdlib"))
  api(gradleTestKit())
  api(libs.truth)

  implementation(platform(libs.kotlin.bom))
  implementation(libs.errorProne) {
    because("Uses @CanIgnoreReturnValue")
  }
  implementation(libs.guava)

  dokkaHtmlPlugin(libs.kotlin.dokka)
}
