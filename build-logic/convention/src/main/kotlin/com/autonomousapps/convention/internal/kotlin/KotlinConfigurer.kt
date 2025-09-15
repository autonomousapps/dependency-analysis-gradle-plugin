// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.convention.internal.kotlin

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class KotlinConfigurer(private val project: Project) {

  private val versionCatalog = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

  private val dokka = versionCatalog.findLibrary("kotlin.dokka").get()
  private val javaTarget = versionCatalog.findVersion("javaTarget").orElseThrow().requiredVersion
  private val kotlin = versionCatalog.findVersion("kotlin").get().requiredVersion

  // this function expects strings of the form 2.x, not 2.x.y
  private val kotlinVersion = KotlinVersion.fromVersion(kotlin.substringBeforeLast('.'))

  private val kotlinJvmExtension = project.extensions.getByType(KotlinJvmProjectExtension::class.java)

  fun configure(): Unit = project.run {
    kotlinJvmExtension.explicitApi()

    configureDokka()
    configureKotlinTarget()
    configureKotlinVersion()
  }

  private fun Project.configureDokka() {
    dependencies.add("dokkaHtmlPlugin", dokka)
  }

  private fun Project.configureKotlinTarget() {
    tasks.withType(KotlinCompile::class.java).configureEach { t ->
      t.compilerOptions {
        // Ensure compatibility with Gradle 8.x. See https://docs.gradle.org/9.0.0/userguide/compatibility.html.
        apiVersion.set(kotlinVersion)
        languageVersion.set(kotlinVersion)
        jvmTarget.set(JvmTarget.fromTarget(javaTarget))
        freeCompilerArgs.add(
          // equivalent to JavaCompile's `options.release`
          "-Xjdk-release=$javaTarget",
        )
      }
    }
  }

  /**
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1537#issuecomment-3293306966">Issue 1537</a>
   */
  private fun Project.configureKotlinVersion() {
    configurations.configureEach { c ->
      if (c.isCanBeResolved) {
        c.resolutionStrategy { r ->
          r.eachDependency { details ->
            val requested = details.requested

            if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-stdlib") {
              details.useVersion(kotlin)
              details.because("Downgrading the stdlib for enhanced compatibility")
            }
          }
        }
      }
    }
  }
}
