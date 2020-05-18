package com.autonomousapps.kit

import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * A Gradle project consists of:
 * 1. A root project, with:
 *    1. gradle.properties file
 *    2. Setting script
 *    3. Build script
 *    4. (Optionally) source code
 * 2. Zero or more subprojects
 */
class GradleProject(
  val rootDir: File,
  val rootProject: RootProject,
  val subprojects: List<Subproject> = emptyList()
) {

  fun writer() = GradleProjectWriter(this)

  fun projectDir(subprojectName: String): Path {
    return projectDir(forName(subprojectName))
  }

  private fun forName(subprojectName: String): Subproject {
    return subprojects.find { it.name == subprojectName }
      ?: throw IllegalStateException("No subproject with name $subprojectName")
  }

  fun projectDir(subproject: Subproject): Path {
    return rootDir.toPath().resolve("${subproject.name}/")
  }

  fun buildDir(subprojectName: String): Path {
    return buildDir(forName(subprojectName))
  }

  fun buildDir(subproject: Subproject): Path {
    return projectDir(subproject).resolve("build/")
  }

  class Builder {
    private var rootProjectBuilder: RootProject.Builder = defaultRootProjectBuilder()
    private val subprojects: MutableList<Subproject> = mutableListOf()

    fun withRootProject(block: RootProject.Builder.() -> Unit) {
      rootProjectBuilder = defaultRootProjectBuilder().apply {
        block(this)
      }
    }

    fun withSubproject(name: String, block: Subproject.Builder.() -> Unit) {
      subprojects += with(Subproject.Builder()) {
        this.name = name
        block(this)
        build()
      }
    }

    fun withAndroidSubproject(name: String, block: AndroidSubproject.Builder.() -> Unit) {
      subprojects += with(AndroidSubproject.Builder()) {
        this.name = name
        block(this)
        build()
      }
    }

    private fun defaultRootProjectBuilder(): RootProject.Builder {
      return RootProject.Builder().apply {
        variant = ":"
        gradleProperties = GradleProperties.DEFAULT
        settingsScript = SettingsScript()
        buildScript = defaultRootProjectBuildScript()
        sources = emptyList()
      }
    }

    private fun defaultRootProjectBuildScript(): BuildScript {
      return BuildScript(
        plugins = listOf(Plugin.dependencyAnalysisPlugin, Plugin.kotlinPlugin(apply = false))
      )
    }

    fun build(): GradleProject {
      val rootProject = rootProjectBuilder.apply {
        settingsScript = SettingsScript(subprojects = subprojects.map { it.name })
      }.build()

      return GradleProject(
        rootDir = File("build/functionalTest/${UUID.randomUUID()}"),
        rootProject = rootProject,
        subprojects = subprojects
      )
    }
  }
}
