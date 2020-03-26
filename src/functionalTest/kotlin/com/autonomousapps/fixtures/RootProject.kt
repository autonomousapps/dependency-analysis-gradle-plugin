package com.autonomousapps.fixtures

import java.io.File

/**
 * Typical root project of an Android build. Contains a `settings.gradle` and `build.gradle`. [agpVersion] will be null
 * for a [MultiModuleJavaLibraryProject].
 */
class RootProject(
    librarySpecs: List<LibrarySpec>? = null,
    agpVersion: String? = null,
    extensionSpec: String = ""
) : RootGradleProject(File(WORKSPACE)) {

  override val variant: String? = null

  init {
    withGradlePropertiesFile("""
            # Necessary for AGP 3.6+
            android.useAndroidX=true
            """.trimIndent())

    withSettingsFile("""
            |rootProject.name = 'real-app'
            |
            |// If agpVersion is null, assume this is a pure Java/Kotlin project, and no app module.
            |${agpVersion?.let { "include(':app')" } ?: ""}
            |${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" }}
        """.trimMargin("|"))

    withBuildFile("""
            |buildscript {
            |    repositories {
            |        google()
            |        jcenter()
            |        maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
            |    }
            |    dependencies {
            |        ${agpVersion?.let { "classpath 'com.android.tools.build:gradle:$it'" } ?: ""}
            |        ${kotlinGradlePlugin(librarySpecs)}
            |    }
            |}
            |plugins {
            |    id('com.autonomousapps.dependency-analysis')
            |}
            |subprojects {
            |    repositories {
            |        google()
            |        jcenter()
            |        maven { url = "https://dl.bintray.com/kotlin/kotlin-eap" }
            |    }
            |}
            |
            |$extensionSpec
        """)
  }

  private fun kotlinGradlePlugin(librarySpecs: List<LibrarySpec>?): String {
    val anyKotlin = librarySpecs?.any {
      it.type == LibraryType.KOTLIN_ANDROID_LIB || it.type == LibraryType.KOTLIN_JVM_LIB
    } ?: false

    return if (anyKotlin) {
      "classpath \"org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.70\""
    } else {
      ""
    }
  }
}
