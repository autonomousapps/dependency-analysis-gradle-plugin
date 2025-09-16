// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm")
  id("com.autonomousapps.dependency-analysis")
}

kotlin {
  explicitApi()
}

// https://github.com/gradle/gradle/issues/22600
tasks.withType<ValidatePlugins>().configureEach {
  enableStricterValidation = true
}

tasks {
  withType<GroovyCompile>().configureEach {
    options.release = libs.versions.javaTarget.map(String::toInt)
  }
  withType<JavaCompile>().configureEach {
    options.release = libs.versions.javaTarget.map(String::toInt)
  }
  withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget = libs.versions.javaTarget.map(JvmTarget::fromTarget)
      freeCompilerArgs = listOf(
        "-Xinline-classes",
        "-opt-in=kotlin.RequiresOptIn",
        "-Xsam-conversions=class",
      )
    }
  }
}

gradlePlugin {
  plugins {
    create("libJava") {
      id = "build-logic.lib.java"
      implementationClass = "com.autonomousapps.convention.LibJavaConventionPlugin"
    }
    create("libKotlin") {
      id = "build-logic.lib.kotlin"
      implementationClass = "com.autonomousapps.convention.LibKotlinConventionPlugin"
    }
    create("plugin") {
      id = "build-logic.plugin"
      implementationClass = "com.autonomousapps.convention.PluginConventionPlugin"
    }
  }
}

dependencies {
  api(libs.javax.inject)
  api(libs.mavenPublishPlugin)

  implementation(platform(libs.kotlin.bom))
  implementation(libs.dependencyAnalysisPlugin)
  implementation(libs.gradleTestKitPlugin)
  implementation(libs.gradle.publish.plugin) {
    because("For extending Gradle Plugin-Publish Plugin functionality")
  }

  // Need to use embedded version for Gradle 9 compatibility.
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${embeddedKotlinVersion}") {
    because("For applying the kotlin-jvm plugin")
  }
  implementation(libs.kotlinDokkaGradlePlugin)
  implementation(libs.shadowGradlePlugin)
}

// These exclusions are about build-logic exposing various dependencies deliberately to client projects. Some of these
// could be runtimeOnly, but I don't feel like dealing with that now.
dependencyAnalysis {
  issues {
    onUnusedDependencies {
      exclude(
        libs.dependencyAnalysisPlugin,
        libs.gradleTestKitPlugin,
        libs.gradle.publish.plugin,
        libs.kotlinDokkaGradlePlugin,
        libs.shadowGradlePlugin,
      )
    }
    onUsedTransitiveDependencies {
      // TODO(tsr): missing DAGP feature around plugin marker artifacts?
      exclude(
        "com.gradle.publish:plugin-publish-plugin",
        "com.gradleup.shadow:shadow-gradle-plugin",
        "org.jetbrains.dokka:dokka-gradle-plugin",
      )
    }
  }
}
