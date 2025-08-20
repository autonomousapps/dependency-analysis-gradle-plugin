// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention.internal.kotlin

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal fun Project.configureDokka(versionCatalog: VersionCatalog) {
  val dokka = versionCatalog.findLibrary("kotlin.dokka").get()
  dependencies.add("dokkaHtmlPlugin", dokka)
}

internal fun Project.configureKotlin(versionCatalog: VersionCatalog) {
  extensions.getByType(KotlinJvmProjectExtension::class.java)
    .explicitApi()

  val javaTarget = versionCatalog.findVersion("javaTarget").orElseThrow().requiredVersion

  tasks.withType(KotlinCompile::class.java).configureEach { t ->
    t.compilerOptions {
      jvmTarget.set(JvmTarget.fromTarget(javaTarget))
      freeCompilerArgs.add(
        "-Xjdk-release=$javaTarget",
      )
    }
  }
}
