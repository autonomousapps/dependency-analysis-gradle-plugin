// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.dependencyAnalysis)
  id("org.jetbrains.kotlin.jvm") version embeddedKotlinVersion apply false
  id("org.jetbrains.dokka") version "2.0.0" apply false
  id("com.autonomousapps.testkit") version "0.13" apply false
  id("com.gradleup.shadow") version "8.3.6" apply false
  id("com.gradle.plugin-publish") version "1.1.0" apply false
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
