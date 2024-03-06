// Copyright (c) 2024. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.WORKSPACE
import java.io.File

fun newSimpleProject(projectVersion: String): File {
  val rootDir = File(WORKSPACE)
  rootDir.mkdirs()

  val buildSrc = rootDir.resolve("buildSrc")
  buildSrc.mkdirs()
  buildSrc.resolve("settings.gradle").writeText("")
  buildSrc.resolve("build.gradle").writeText("""
        repositories {
            gradlePluginPortal()
            maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
            mavenCentral()
        }
        dependencies {
            // This forces download of the actual binary plugin, rather than using what is bundled with project
            implementation "com.autonomousapps:dependency-analysis-gradle-plugin:${projectVersion}"
        }
        """.trimIndent())

  rootDir.resolve("settings.gradle").writeText("""
            rootProject.name = 'smoke-test'
            """.trimIndent())

  rootDir.resolve("build.gradle").writeText("""
        plugins {
            id 'com.autonomousapps.dependency-analysis'
        }
        repositories {
            mavenCentral()
        }
        """.trimIndent())

  return rootDir
}
