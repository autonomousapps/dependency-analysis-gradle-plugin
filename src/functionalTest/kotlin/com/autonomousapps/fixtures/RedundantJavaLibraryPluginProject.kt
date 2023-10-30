package com.autonomousapps.fixtures

import com.autonomousapps.advice.PluginAdvice
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.dependencies.Plugins
import com.autonomousapps.model.ProjectAdvice
import java.io.File

class RedundantKotlinJvmPluginProject @JvmOverloads constructor(
  includeKotlin: Boolean = false,
) : ProjectDirProvider {

  private val rootSpecWithJava = RootSpec(
    buildScript = buildScript(),
    sources = setOf(
      Source(
        path = DEFAULT_PACKAGE_PATH,
        name = "MyClass.java",
        source = """
        package $DEFAULT_PACKAGE_NAME;
        
        public class MyClass {
        }
      """.trimIndent()
      )
    )
  )

  private val rootSpecWithKotlin = RootSpec(
    buildScript = buildScript(),
    sources = setOf(
      Source(
        path = DEFAULT_PACKAGE_PATH,
        name = "MyClass.kt",
        source = """
        package $DEFAULT_PACKAGE_NAME
        
        class MyClass
      """.trimIndent()
      )
    )
  )

  private val rootProject = if (includeKotlin) RootProject(rootSpecWithKotlin) else RootProject(rootSpecWithJava)

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
          id 'org.jetbrains.kotlin.jvm' version '${Plugins.KOTLIN_VERSION}'
          id 'java-library'
          id 'com.autonomousapps.dependency-analysis' version '${
        System.getProperty("com.autonomousapps.plugin-under-test.version")
      }'
        }
        
        repositories {
          google()
          mavenCentral()
        }
        
        dependencies {
          implementation "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}"
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice(): Set<ProjectAdvice> {
      return setOf(
        ProjectAdvice(
          projectPath = ":",
          pluginAdvice = setOf(PluginAdvice.redundantKotlinJvm())
        )
      )
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
          id 'org.jetbrains.kotlin.jvm' version '${Plugins.KOTLIN_VERSION}'
          id 'java-library'
          id 'org.jetbrains.kotlin.kapt' version '${Plugins.KOTLIN_VERSION}'
          id 'com.autonomousapps.dependency-analysis' version '${
        System.getProperty("com.autonomousapps.plugin-under-test.version")
      }'
        }
        
        repositories {
          google()
          mavenCentral()
        }
        
        dependencies {
          implementation "org.jetbrains.kotlin:kotlin-stdlib:${Plugins.KOTLIN_VERSION}"
        }
    """.trimIndent()
    }

    @JvmStatic
    fun expectedAdvice(): Set<ProjectAdvice> {
      return setOf(
        ProjectAdvice(
          projectPath = ":",
          pluginAdvice = setOf(PluginAdvice.redundantKotlinJvm(), PluginAdvice.redundantKapt())
        )
      )
    }
  }
}
