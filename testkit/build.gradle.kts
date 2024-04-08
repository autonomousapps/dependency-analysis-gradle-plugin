// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.dependencyAnalysis)
  id("org.jetbrains.kotlin.jvm") apply false
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
