package com.autonomousapps.fixtures

import com.autonomousapps.advice.PluginAdvice
import java.io.File

class RedundantJvmPluginsProject : ProjectDirProvider {

  private val rootSpec = RootSpec(buildScript = buildScript())

  private val rootProject = RootProject(rootSpec)

  override val projectDir: File = rootProject.projectDir

  override fun project(moduleName: String): Module {
    if (moduleName == ":") {
      return rootProject
    } else {
      error("No '$moduleName' project found!")
    }
  }

  companion object {
    // The point. This project has both kotlin-jvm and java-library applied, which is redundant.
    private fun buildScript(): String {
      return """
        plugins {
          id 'org.jetbrains.kotlin.jvm' version '1.3.71'
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis'
        }
        
        repositories {
          google()
          mavenCentral()
          jcenter()
        }
        
        dependencies {
          implementation "org.jetbrains.kotlin:kotlin-stdlib:1.3.71"
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice() = mapOf(":" to listOf(PluginAdvice.redundantPlugin()))
  }
}
