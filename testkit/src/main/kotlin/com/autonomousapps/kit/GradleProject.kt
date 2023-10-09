package com.autonomousapps.kit

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.android.*
import com.autonomousapps.kit.gradle.*
import java.io.File
import java.nio.file.Path

/**
 * A Gradle project consists of:
 * 1. (Optionally) buildSrc
 * 2. A root project, with:
 *    1. gradle.properties file
 *    2. Setting script
 *    3. Build script
 *    4. (Optionally) source code
 * 3. Zero or more included builds
 * 4. Zero or more subprojects
 *
 * And it is declared using either Groovy or Kotlin DSL (see [DslKind]).
 */
class GradleProject(
  val rootDir: File,
  val dslKind: DslKind,
  val buildSrc: Subproject?,
  val rootProject: RootProject,
  val includedBuilds: List<RootProject> = emptyList(),
  val subprojects: List<Subproject> = emptyList(),
) {

  enum class DslKind {
    GROOVY,
    KOTLIN
  }

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
    return rootDir.toPath().resolve("${project.includedBuild?.let { "$it/" } ?: ""}${project.name.replace(":", "/")}/")
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
    @JvmOverloads
    @JvmStatic
    fun minimalAndroidProject(
      rootDir: File,
      agpVersion: String,
      dslKind: DslKind = DslKind.GROOVY,
    ): Builder {
      return Builder(rootDir, dslKind).apply {
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
            dependencies = listOf(com.autonomousapps.kit.gradle.Dependency.appcompat("implementation"))
          }
        }
      }
    }
  }

  class Builder @JvmOverloads constructor(
    private val rootDir: File,
    private val dslKind: DslKind = DslKind.GROOVY,
  ) {
    private var buildSrcBuilder: Subproject.Builder? = null
    private var rootProjectBuilder: RootProject.Builder = defaultRootProjectBuilder()
    private var includedProjectMap: MutableMap<String, RootProject.Builder> = mutableMapOf()
    private val subprojectMap: MutableMap<String, Subproject.Builder> = mutableMapOf()
    private val androidSubprojectMap: MutableMap<String, AndroidSubproject.Builder> = mutableMapOf()

    fun withBuildSrc(block: Subproject.Builder.() -> Unit) {
      val builder = Subproject.Builder()
      builder.apply {
        this.name = "buildSrc"
        block(this)
      }
      buildSrcBuilder = builder
    }

    fun withRootProject(block: RootProject.Builder.() -> Unit) {
      rootProjectBuilder = rootProjectBuilder.apply {
        block(this)
      }
    }

    fun withIncludedBuild(name: String, block: RootProject.Builder.() -> Unit) {
      // If a builder with this name already exists, returning it for building-upon
      val builder = includedProjectMap[name] ?: defaultRootProjectBuilder()
      builder.apply {
        settingsScript = SettingsScript(rootProjectName = name)
        block(this)
      }
      includedProjectMap[name] = builder
    }

    fun withSubproject(name: String, block: Subproject.Builder.() -> Unit) {
      val normalizedName = name.removePrefix(":")
      // If a builder with this name already exists, returning it for building-upon
      val builder = subprojectMap[normalizedName] ?: Subproject.Builder()
      builder.apply {
        this.name = normalizedName
        block(this)
      }
      subprojectMap[normalizedName] = builder
    }

    fun withSubprojectInIncludedBuild(includedBuild: String, name: String, block: Subproject.Builder.() -> Unit) {
      val builder = includedProjectMap[includedBuild] ?: defaultRootProjectBuilder()
      builder.apply {
        settingsScript = SettingsScript(
          rootProjectName = includedBuild,
          subprojects = settingsScript.subprojects + name
        )
      }
      includedProjectMap[includedBuild] = builder

      withSubproject(name) {
        this.includedBuild = includedBuild
        block(this)
      }
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

    fun withAndroidLibProject(name: String, packageName: String, block: AndroidSubproject.Builder.() -> Unit) {
      // If a builder with this name already exists, returning it for building-upon
      val builder = androidSubprojectMap[name] ?: AndroidSubproject.Builder()
      builder.apply {
        this.name = name
        this.manifest = AndroidManifest.defaultLib(packageName)
        this.styles = AndroidStyleRes.EMPTY
        this.colors = AndroidColorRes.EMPTY
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
        plugins = Plugins(listOf(Plugin.dependencyAnalysisPlugin, Plugin.kotlinPlugin(apply = false)))
      )
    }

    fun build(): GradleProject {
      val subprojectNames = subprojectMap.filter { it.value.includedBuild == null }.keys + androidSubprojectMap.keys
      val rootProject = rootProjectBuilder.apply {
        settingsScript.subprojects = subprojectNames
      }.build()

      val includedBuilds = includedProjectMap.map { it.value.build() }

      val subprojects = subprojectMap.map { it.value.build() } +
        androidSubprojectMap.map { it.value.build() }

      return GradleProject(
        rootDir = rootDir,
        dslKind = dslKind,
        buildSrc = buildSrcBuilder?.build(),
        rootProject = rootProject,
        includedBuilds = includedBuilds,
        subprojects = subprojects
      )
    }
  }
}
