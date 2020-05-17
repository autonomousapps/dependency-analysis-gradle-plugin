package com.autonomousapps.fixtures

import com.autonomousapps.advice.Advice
import com.autonomousapps.advice.Dependency
import java.io.File

class JvmDaggerProject : ProjectDirProvider {

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
    private fun buildScript(): String {
      return """
        plugins {
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'
        }
        
        java {
          sourceCompatibility = JavaVersion.VERSION_1_8
          targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        repositories {
          google()
          mavenCentral()
          jcenter()
        }
        
        dependencies {
          implementation 'com.google.dagger:dagger:2.24'
          annotationProcessor 'com.google.dagger:dagger-compiler:2.24'
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice() = setOf(
      Advice.ofRemove(Dependency("com.google.dagger:dagger", "2.24", "implementation")),
      Advice.ofRemove(Dependency("com.google.dagger:dagger-compiler", "2.24", "annotationProcessor"))
    )
  }
}
