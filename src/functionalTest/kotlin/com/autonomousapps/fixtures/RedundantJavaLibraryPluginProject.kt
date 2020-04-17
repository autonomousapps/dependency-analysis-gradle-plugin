package com.autonomousapps.fixtures

import com.autonomousapps.advice.BuildHealth
import com.autonomousapps.advice.PluginAdvice
import java.io.File

class RedundantJavaLibraryPluginProject : ProjectDirProvider {

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
    fun expectedAdvice(): Set<BuildHealth> {
      return setOf(BuildHealth(
        projectPath = ":",
        dependencyAdvice = emptySet(),
        pluginAdvice = setOf(PluginAdvice.redundantJavaLibrary())
      ))
    }
  }
}

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
    fun expectedAdvice(): Set<BuildHealth> {
      return setOf(BuildHealth(
        projectPath = ":",
        dependencyAdvice = emptySet(),
        pluginAdvice = setOf(PluginAdvice.redundantKotlinJvm())
      ))
    }
  }
}

class RedundantJavaLibraryAndKaptPluginsProject : ProjectDirProvider {

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
          id 'org.jetbrains.kotlin.kapt' version '1.3.71'
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
    fun expectedAdvice(): Set<BuildHealth> {
      return setOf(BuildHealth(
        projectPath = ":",
        dependencyAdvice = emptySet(),
        pluginAdvice = setOf(PluginAdvice.redundantJavaLibrary(), PluginAdvice.redundantKapt())
      ))
    }
  }
}
