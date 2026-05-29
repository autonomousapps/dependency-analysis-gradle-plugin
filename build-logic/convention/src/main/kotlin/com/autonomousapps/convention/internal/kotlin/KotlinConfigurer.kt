// Copyright (c) 2026. Tony Robalik.
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
  private val javaTarget = versionCatalog.findVersion("javaTarget").orElseThrow().requiredVersion

  /** Version of the bundled `kotlin-stdlib*`/`kotlin-reflect`. Capped for Gradle 8.x compatibility (see `libs.versions.toml`). */
  private val kotlinRuntime = versionCatalog.findVersion("kotlinRuntime").get().requiredVersion
  private val kotlinLanguageVersion = versionCatalog.findVersion("kotlinLanguageVersion").get().requiredVersion
    .toKotlinVersion()

  fun configure(): Unit = project.run {
    configureKotlinExtension()
    configureKotlinTarget()
    configureKotlinVersion()
  }

  private fun String.toKotlinVersion(): KotlinVersion = KotlinVersion.fromVersion(this)

  private fun Project.configureKotlinExtension() {
    project.extensions.getByType(KotlinJvmProjectExtension::class.java).run {
      explicitApi()
      // Bundled stdlib is capped for Gradle 8.x compatibility; compile against that same API level.
      coreLibrariesVersion = kotlinRuntime
    }
  }

  private fun Project.configureKotlinTarget() {
    tasks.withType(KotlinCompile::class.java).configureEach { t ->
      t.compilerOptions {
        // Ensure compatibility with various versions of Gradle.
        // See https://docs.gradle.org/9.4.1/userguide/compatibility.html.
        apiVersion.set(kotlinLanguageVersion)
        languageVersion.set(kotlinLanguageVersion)
        jvmTarget.set(JvmTarget.fromTarget(javaTarget))
        freeCompilerArgs.add(
          // equivalent to JavaCompile's `options.release`
          "-Xjdk-release=$javaTarget",
        )
      }
    }
  }

  /**
   * Pin the bundled Kotlin runtime libraries (`kotlin-stdlib*`, `kotlin-reflect`) to [kotlinRuntime]. These end up on
   * DAGP's shaded runtime classpath, so their binary-metadata version must stay readable by the oldest supported
   * Gradle's Kotlin DSL script compiler.
   *
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1537#issuecomment-3293306966">Issue 1537</a>
   * @see <a href="https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1671">Issue 1671</a>
   */
  private fun Project.configureKotlinVersion() {
    configurations.configureEach { c ->
      if (c.isCanBeResolved) {
        c.resolutionStrategy { r ->
          r.eachDependency { details ->
            val requested = details.requested

            if (
              requested.group == "org.jetbrains.kotlin" &&
              (requested.name.startsWith("kotlin-stdlib") || requested.name == "kotlin-reflect")
            ) {
              details.useVersion(kotlinRuntime)
              details.because("Downgrading the Kotlin runtime libraries for enhanced compatibility (issues 1537, 1671)")
            }
          }
        }
      }
    }
  }
}
