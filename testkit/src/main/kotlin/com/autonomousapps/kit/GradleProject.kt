package com.autonomousapps.kit

import java.io.File
import java.nio.file.Path

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
  val subprojects: Set<Subproject> = emptySet()
) {

  fun writer() = GradleProjectWriter(this)

  /**
   * Use ":" for the root project.
   */
  fun projectDir(projectName: String): Path {
    return projectDir(forName(projectName))
  }

  /**
   * Use [rootProject] for the root project.
   */
  fun projectDir(project: Subproject): Path {
    if (project == rootProject) {
      return rootDir.toPath()
    }
    return rootDir.toPath().resolve("${project.name}/")
  }

  /**
   * Use ":" for the root project.
   */
  fun buildDir(projectName: String): Path {
    return buildDir(forName(projectName))
  }

  /**
   * Use [rootProject] for the root project.
   */
  fun buildDir(project: Subproject): Path {
    return projectDir(project).resolve("build/")
  }

  private fun forName(projectName: String): Subproject {
    if (projectName == ":") {
      return rootProject
    }

    return subprojects.find { it.name == projectName }
      ?: throw IllegalStateException("No subproject with name $projectName")
  }

  companion object {
    /**
     * Returns a [Builder] for an Android project with a single "app" module. Call [Builder.build]
     * on the returned object to create the test fixture.
     */
    @JvmStatic
    fun minimalAndroidProject(rootDir: File, agpVersion: String): Builder {
      return Builder(rootDir).apply {
        withRootProject {
          gradleProperties = GradleProperties.minimalAndroidProperties()
          withBuildScript {
            buildscript = BuildscriptBlock.defaultAndroidBuildscriptBlock(agpVersion)
          }
        }
        withAndroidSubproject("app") {
          manifest = AndroidManifest.app(application = null, activities = emptyList())
          withBuildScript {
            plugins = mutableListOf(Plugin.androidAppPlugin)
            android = AndroidBlock.defaultAndroidAppBlock(isKotlinApplied = false)
            dependencies = listOf(Dependency.appcompat("implementation"))
          }
        }
      }
    }
  }

  class Builder(private val rootDir: File) {
    private var rootProjectBuilder: RootProject.Builder = defaultRootProjectBuilder()
    private val subprojectMap: MutableMap<String, Subproject.Builder> = mutableMapOf()
    private val androidSubprojectMap: MutableMap<String, AndroidSubproject.Builder> = mutableMapOf()

    fun withRootProject(block: RootProject.Builder.() -> Unit) {
      rootProjectBuilder = rootProjectBuilder.apply {
        block(this)
      }
    }

    fun withSubproject(name: String, block: Subproject.Builder.() -> Unit) {
      // If a builder with this name already exists, returning it for building-upon
      val builder = subprojectMap[name] ?: Subproject.Builder()
      builder.apply {
        this.name = name
        block(this)
      }
      subprojectMap[name] = builder
    }

    fun withAndroidSubproject(name: String, block: AndroidSubproject.Builder.() -> Unit) {
      // If a builder with this name already exists, returning it for building-upon
      val builder = androidSubprojectMap[name] ?: AndroidSubproject.Builder()
      builder.apply {
        this.name = name
        block(this)
      }
      androidSubprojectMap[name] = builder
    }

    private fun defaultRootProjectBuilder(): RootProject.Builder {
      return RootProject.Builder().apply {
        variant = ":"
        gradleProperties = GradleProperties.minimalJvmProperties()
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
      val subprojectNames = subprojectMap.keys + androidSubprojectMap.keys
      val rootProject = rootProjectBuilder.apply {
        settingsScript = SettingsScript(subprojects = subprojectNames)
      }.build()

      val subprojects = subprojectMap.map { it.value.build() } +
        androidSubprojectMap.map { it.value.build() }

      return GradleProject(
        rootDir = rootDir,
        rootProject = rootProject,
        subprojects = subprojects.toSet()
      )
    }
  }
}
