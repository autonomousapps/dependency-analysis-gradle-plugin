// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
rootProject.name = "testkit"

pluginManagement {
  includeBuild("../build-logic")

  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }

    gradlePluginPortal()
    mavenCentral()

    // This is for the `com.android.tools.metalava:metalava` dependency
    exclusiveContent {
      forRepository {
        maven(url = "https://dl.google.com/dl/android/maven2/")
      }
      filter {
        includeGroup("com.android.tools.metalava")
        includeGroup("com.android.tools")
        includeGroup("com.android.tools.layoutlib")
        includeGroup("com.android.tools.ddms")
        includeGroup("com.android.tools.build")
        includeGroup("com.android.tools.analytics-library")
        includeGroup("com.android.tools.lint")
        includeGroup("com.android.tools.external.com-intellij")
        includeGroup("com.android.tools.external.org-jetbrains")
      }
    }

    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
  }
}

plugins {
  id("com.gradle.develocity") version "4.5.1"
  id("com.gradle.common-custom-user-data-gradle-plugin") version "2.8.0"
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositories {
    // -Dlocal
    if (providers.systemProperty("local").isPresent) {
      mavenLocal()
    }

    google()
    mavenCentral()

    // snapshots are permitted, but only for dependencies I own
    maven {
      url = uri("https://central.sonatype.com/repository/maven-snapshots/")
      content {
        includeGroup("com.autonomousapps")
        includeGroup("com.autonomousapps.dependency-analysis")
      }
    }
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

val isCI = System.getenv("CI") != null

develocity {
  server = "https://community.develocity.cloud"
  projectId = "autonomousapps"

  buildScan {
    uploadInBackground = !isCI
    publishing.onlyIf { it.isAuthenticated }
    obfuscation {
      ipAddresses { addresses -> addresses.map { _ -> "0.0.0.0" } }
    }
  }
}

buildCache {
  local {
    isEnabled = true
  }

  remote(develocity.buildCache) {
    isEnabled = true
    // Check access key presence to avoid build cache errors on PR builds when access key is not present
    val accessKey = System.getenv("DEVELOCITY_ACCESS_KEY")
    isPush = isCI && accessKey != null
  }
}

include(":gradle-testkit-plugin")
include(":gradle-testkit-support")
include(":gradle-testkit-truth")
