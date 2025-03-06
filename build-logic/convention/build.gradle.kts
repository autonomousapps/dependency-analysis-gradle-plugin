// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.java.get())
    freeCompilerArgs = listOf("-Xinline-classes", "-opt-in=kotlin.RequiresOptIn", "-Xsam-conversions=class")
  }
}

gradlePlugin {
  plugins {
    create("build-logic") {
      id = "convention"
      implementationClass = "com.autonomousapps.convention.ConventionPlugin"
    }
  }
}

dependencies {
  implementation(platform(libs.kotlin.bom))

  // https://docs.google.com/document/d/1tylyteny2wjUfB26Kdn2WKP3pBNjatyNMbz9CFFhpko/edit?tab=t.0
  implementation("org.gradle.experimental:gradle-public-api:8.13-rc-2")

  implementation(libs.gradle.publish.plugin) {
    because("For extending Gradle Plugin-Publish Plugin functionality")
  }
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion") {
    because("For applying the kotlin-jvm plugin")
  }
  implementation(libs.moshi.core) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.moshi.kotlin) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.okhttp3) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.retrofit.converter.moshi) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
  implementation(libs.retrofit.core) {
    because("Closing and releasing Sonatype Nexus staging repo")
  }
}
