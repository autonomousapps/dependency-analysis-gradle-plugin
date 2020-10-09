@file:Suppress("PropertyName")

plugins {
  id("com.gradle.enterprise") version "3.4.1"
}

val VERSION: String by extra.properties

gradleEnterprise {
  buildScan {
    publishAlways()
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")
    tag(VERSION)

    val githubActionID = System.getenv("GITHUB_ACTION")

    if (!githubActionID.isNullOrBlank()) {
      link(
        "WorkflowURL",
        "https://github.com/" +
          System.getenv("GITHUB_REPOSITORY") +
          "/pull/" +
          System.getenv("PR_NUMBER") +
          "/checks?check_run_id=" +
          System.getenv("GITHUB_RUN_ID")
      )
    }
  }
}

rootProject.name = "dependency-analysis-gradle-plugin"

include(":antlr")
include(":testkit")

// https://docs.gradle.org/5.6/userguide/groovy_plugin.html#sec:groovy_compilation_avoidance
enableFeaturePreview("GROOVY_COMPILATION_AVOIDANCE")
