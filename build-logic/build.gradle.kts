// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  `kotlin-dsl`
  alias(libs.plugins.dependencyAnalysis)
}

dependencyAnalysis {
  reporting {
    printBuildHealth(true)
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
