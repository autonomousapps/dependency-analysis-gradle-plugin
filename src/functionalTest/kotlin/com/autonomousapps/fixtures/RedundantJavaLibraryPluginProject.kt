package com.autonomousapps.fixtures

import com.autonomousapps.advice.ComprehensiveAdvice
import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.Plugin
import java.io.File

class RedundantKotlinJvmPluginProject : ProjectDirProvider {

  private val rootSpec = RootSpec(
    buildScript = buildScript(),
    sources = setOf(Source(
      path = DEFAULT_PACKAGE_PATH,
      name = "MyClass.java",
      source = """
        package $DEFAULT_PACKAGE_NAME;
        
        public class MyClass {
        }
      """.trimIndent()
    ))
  )

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
          id 'org.jetbrains.kotlin.jvm' version '${Plugin.KOTLIN_VERSION}'
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'
        }
        
        repositories {
          google()
          mavenCentral()
        }
        
        dependencies {
          implementation "org.jetbrains.kotlin:kotlin-stdlib:${Plugin.KOTLIN_VERSION}"
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice(): Set<ComprehensiveAdvice> {
      return setOf(ComprehensiveAdvice(
        projectPath = ":",
        dependencyAdvice = emptySet(),
        pluginAdvice = setOf(PluginAdvice.redundantKotlinJvm())
      ))
    }
  }
}

class RedundantKotlinJvmAndKaptPluginsProject : ProjectDirProvider {

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
          id 'org.jetbrains.kotlin.jvm' version '${Plugin.KOTLIN_VERSION}'
          id 'java-library'
          id 'org.jetbrains.kotlin.kapt' version '${Plugin.KOTLIN_VERSION}'
          id 'com.autonomousapps.dependency-analysis' version '${System.getProperty("com.autonomousapps.pluginversion")}'
        }
        
        repositories {
          google()
          mavenCentral()
        }
        
        dependencies {
          implementation "org.jetbrains.kotlin:kotlin-stdlib:${Plugin.KOTLIN_VERSION}"
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice(): Set<ComprehensiveAdvice> {
      return setOf(ComprehensiveAdvice(
        projectPath = ":",
        dependencyAdvice = emptySet(),
        pluginAdvice = setOf(PluginAdvice.redundantKotlinJvm(), PluginAdvice.redundantKapt())
      ))
    }
  }
}
