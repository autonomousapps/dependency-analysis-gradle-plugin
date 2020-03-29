package com.autonomousapps.fixtures

import com.autonomousapps.WORKSPACE
import java.io.File

internal class MultiModuleJavaProject(
    private val projectVersion: String,
    private val extension: String = ""
) {

  internal val rootDir = File(WORKSPACE).also { it.mkdirs() }

  private val subprojects = listOf("lib1", "lib2")

  init {
    buildSrc()
    settings()
    buildscript()
    subprojects.forEach {
      JavaLibrary(
          libDir = rootDir.resolve(it),
          sources = listOf(JavaLibSpec(
              srcDir = "src/main/java/com/smoketest/",
              className = it.capitalize(),
              classContent = """
                        package com.smoketest;
                    
                        public class ${it.capitalize()} {
                    
                        }
                    """.trimIndent()
          ))
      )
    }
  }

  private fun buildSrc() {
    val buildSrc = rootDir.resolve("buildSrc")
    buildSrc.mkdirs()
    buildSrc.resolve("settings.gradle").writeText("")
    buildSrc.resolve("build.gradle").writeText("""
            repositories {
                gradlePluginPortal()
                maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
                jcenter()
            }
            dependencies {
                // This forces download of the actual binary plugin, rather than using what is bundled with project
                implementation "com.autonomousapps:dependency-analysis-gradle-plugin:${projectVersion}"
            }
            """.trimIndent())
  }

  private fun settings() {
    rootDir.resolve("settings.gradle").writeText("""
            rootProject.name = 'smoke-test'
            
            ${subprojects.joinToString(separator = "\n") { "include ':$it'" }}
            """.trimIndent())
  }

  private fun buildscript() {
    rootDir.resolve("build.gradle").writeText("""
            plugins {
                id 'com.autonomousapps.dependency-analysis'
            }
            repositories {
                jcenter()
            }
            $extension
            """.trimIndent())
  }

  private class JavaLibrary(
      private val libDir: File,
      private val dependencies: List<String> = emptyList(),
      private val sources: List<JavaLibSpec> = emptyList()
  ) {
    init {
      libDir.mkdirs()
      buildscript()
      sources()
    }

    private fun buildscript() {
      libDir.resolve("build.gradle").writeText("""
                plugins {
                    id 'java-library'
                }
                repositories {
                    jcenter()
                }
                dependencies {
                    ${dependencies.joinToString(separator = "\n")}
                }
                """.trimIndent())
    }

    private fun sources() {
      sources.forEach { spec ->
        val srcDir = libDir.resolve(spec.srcDir)
        srcDir.mkdirs()
        srcDir.resolve(spec.className).writeText(spec.classContent)
      }
    }
  }

  private class JavaLibSpec(
      val srcDir: String,
      val className: String,
      val classContent: String
  )
}
