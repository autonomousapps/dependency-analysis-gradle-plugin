// Copyright (c) 2026. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("org.jetbrains.kotlin.jvm") version "2.2.21" apply false
  alias(libs.plugins.dependencyAnalysis)
}

dependencyAnalysis {
  reporting {
    printBuildHealth(true)
  }
  structure {
    bundle("kgp") {
      includeDependency("org.jetbrains.kotlin:kotlin-gradle-plugin")
      includeDependency("org.jetbrains.kotlin:kotlin-gradle-plugin-api")
    }
  }
  issues {
    all {
      onAny {
        severity("fail")
      }
      onRedundantPlugins {
        exclude("org.jetbrains.kotlin.jvm")
      }
    }
  }
}
