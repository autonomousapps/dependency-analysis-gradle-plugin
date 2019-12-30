package com.autonomousapps.fixtures

import java.io.File

/**
 * Typical root project of an Android build. Contains a `settings.gradle` and `build.gradle`. [agpVersion] will be null
 * for a [MultiModuleJavaLibraryProject].
 */
class RootProject(librarySpecs: List<LibrarySpec>? = null, agpVersion: String? = null)
    : RootGradleProject(File(WORKSPACE)) {

    override val variant: String? = null

    init {
        withSettingsFile("""
            |rootProject.name = 'real-app'
            |
            |// If agpVersion is null, assume this is a pure Java/Kotlin project, and no app module.
            |${agpVersion?.let { "include(':app')" } ?: ""}
            |${librarySpecs?.map { it.name }?.joinToString("\n") { "include(':$it')" }}
        """.trimMargin("|"))

        withBuildFile("""
            buildscript {
                repositories {
                    google()
                    jcenter()
                }
                dependencies {
                    ${agpVersion?.let { "classpath 'com.android.tools.build:gradle:$it'" } ?: ""}
                    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61"
                }
            }
            plugins {
                id('com.autonomousapps.dependency-analysis')
            }
            subprojects {
                repositories {
                    google()
                    jcenter()
                }
            }
        """)
    }
}
