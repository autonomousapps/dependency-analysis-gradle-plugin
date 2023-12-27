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
    }
  }
}
