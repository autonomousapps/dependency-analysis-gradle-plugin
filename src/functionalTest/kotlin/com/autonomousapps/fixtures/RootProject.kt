package com.autonomousapps.fixtures

import java.io.File

/**
 * Typical root project of an Android build. Contains a `settings.gradle` and `build.gradle`. [agpVersion] will be null
 * for a [MultiModuleJavaLibraryProject].
 */
class RootProject(
  rootSpec: RootSpec
) : RootGradleProject(File(WORKSPACE)) {

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
  val buildScript: String = defaultBuildScript(agpVersion, librarySpecs, extensionSpec),
  val sources: Set<Source>? = null
) : ModuleSpec {

  override val name: String = ":"

  companion object {
    // For use from Groovy
    @JvmStatic @JvmOverloads fun defaultRootSpec(librarySpecs: List<LibrarySpec>? = null) = RootSpec(librarySpecs)

    @JvmStatic fun defaultGradleProperties() = "# Necessary for AGP 3.6+\nandroid.useAndroidX=true"

    @JvmStatic fun defaultSettingsScript(agpVersion: String?, librarySpecs: List<LibrarySpec>?) = """
      rootProject.name = 'real-app'
      // If agpVersion is null, assume this is a pure Java/Kotlin project, and no app module.
      ${agpVersion?.let { "include(':app')" } ?: ""}
      ${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" } ?: ""}
    """.trimIndent()

    @JvmStatic fun defaultBuildScript(agpVersion: String?, librarySpecs: List<LibrarySpec>?, extensionSpec: String) = """
      buildscript {
        repositories {
          google()
          jcenter()
          maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
        }
        dependencies {
          ${agpVersion?.let { "classpath 'com.android.tools.build:gradle:$it'" } ?: ""}
          ${kotlinGradlePlugin(librarySpecs)}
        }
      }
      plugins {
        id('com.autonomousapps.dependency-analysis')
      }
      subprojects {
        repositories {
          google()
          jcenter()
          maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
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
        """classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.70""""
      } else {
        ""
      }
    }
  }
}
