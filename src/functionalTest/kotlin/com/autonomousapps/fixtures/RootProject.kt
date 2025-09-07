// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.fixtures

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.gradle.dependencies.Plugins
import java.io.File
import java.util.UUID

// Very similar to what is in AbstractProject
private fun newSlug() = buildString {
  append(UUID.randomUUID().toString().take(16))
  System.getProperty("org.gradle.test.worker")?.let { append("-$it") }
}

/**
 * Typical root project of an Android build. Contains a `settings.gradle` and `build.gradle`.
 */
class RootProject(
  rootSpec: RootSpec,
) : RootGradleProject(File("$WORKSPACE/${newSlug()}")) {

  override val variant = "main"

  init {
    withGradlePropertiesFile(rootSpec.gradleProperties)
    withSettingsFile(rootSpec.settingsScript)
    withBuildFile(rootSpec.buildScript)
    withSources(rootSpec.sources)
  }
}

class RootSpec @JvmOverloads constructor(
  private val librarySpecs: List<LibrarySpec>? = null,
  private val extensionSpec: String = "",
  val gradleProperties: String = defaultGradleProperties(),
  val agpVersion: String? = null,
  val settingsScript: String = defaultSettingsScript(agpVersion, librarySpecs),
  val buildScript: String = defaultBuildScript(agpVersion, extensionSpec),
  val sources: Set<Source>? = null,
) : ModuleSpec {

  override val name: String = ":"

  companion object {
    // For use from Groovy
    @JvmStatic @JvmOverloads fun defaultRootSpec(librarySpecs: List<LibrarySpec>? = null) = RootSpec(librarySpecs)

    @JvmStatic fun defaultGradleProperties() = """
      # Necessary for AGP 3.6+
      android.useAndroidX=true
      
      # Try to prevent OOMs (Metaspace) in test daemons spawned by testkit tests
      org.gradle.jvmargs=-Dfile.encoding=UTF-8 -XX:+HeapDumpOnOutOfMemoryError -XX:MaxMetaspaceSize=1024m
    """.trimIndent()

    @JvmStatic fun defaultSettingsScript(agpVersion: String?, librarySpecs: List<LibrarySpec>?) = """
      pluginManagement {
        repositories {
          maven { url = '${System.getProperty("com.autonomousapps.plugin-under-test.repo")}' }
          gradlePluginPortal()
          mavenCentral()
          google()
        }
      }
      
      rootProject.name = 'real-app'
      ${agpVersion?.let { "include(':app')" } ?: ""}
      ${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" } ?: ""}
    """.trimIndent()

    @JvmStatic fun defaultBuildScript(
      agpVersion: String?,
      extensionSpec: String,
    ) = """
      buildscript {
        repositories {
          google()
          mavenCentral()
        }
        dependencies {
          ${agpVersion?.let { "classpath 'com.android.tools.build:gradle:$it'" } ?: ""}
          classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:${Plugins.KOTLIN_ANDROID_VERSION}'
        }
      }
      plugins {
        id('com.autonomousapps.dependency-analysis') version '${AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION}'
      }
      subprojects {
        repositories {
          maven { url = '${System.getProperty("com.autonomousapps.plugin-under-test.repo")}' }
          google()
          mavenCentral()
        }
      }
      $extensionSpec
    """.trimIndent()

    @JvmStatic
    fun kotlinGradlePlugin(librarySpecs: List<LibrarySpec>?): String {
      val anyKotlin = librarySpecs?.any {
        it.type == LibraryType.KOTLIN_ANDROID_LIB || it.type == LibraryType.KOTLIN_JVM_LIB
      } ?: false

      return if (anyKotlin) {
        """classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${Plugins.KOTLIN_VERSION}""""
      } else {
        ""
      }
    }
  }
}
