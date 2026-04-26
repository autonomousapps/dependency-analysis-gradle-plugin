// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.dependencyAnalysis)
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.testkit) apply false
  alias(libs.plugins.shadow) apply false
  alias(libs.plugins.gradlePublishPlugin) apply false
  alias(libs.plugins.buildconfig) apply false
}

// see also `pubLocal` in root build.gradle.kts
tasks.register("pubLocal") {
  group = "publishing"
  description = "Publish all local artifacts to maven local"

  val tasks = subprojects
    .map { p -> p.path }
    .map { path ->
      if (path == ":") {
        ":publishToMavenLocal"
      } else {
        "$path:publishToMavenLocal"
      }
    }

  dependsOn(tasks)
}

dependencyAnalysis {
  issues {
    all {
      onAny {
        severity("fail")
      }
      onIncorrectConfiguration {
        // This is added by KGP by default to functionalTestApi. Ignore it.
        exclude(libs.kotlin.stdlib.core)
      }
    }
  }
}
