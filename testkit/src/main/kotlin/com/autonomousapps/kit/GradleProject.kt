package com.autonomousapps.kit

import com.autonomousapps.kit.GradleProject.DslKind
import com.autonomousapps.kit.android.AndroidColorRes
import com.autonomousapps.kit.android.AndroidManifest
import com.autonomousapps.kit.android.AndroidStyleRes
import com.autonomousapps.kit.android.AndroidSubproject
import com.autonomousapps.kit.gradle.BuildScript
import com.autonomousapps.kit.gradle.GradleProperties
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.gradle.SettingsScript
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
public class GradleProject(
  public val rootDir: File,
  public val dslKind: DslKind,
  public val buildSrc: Subproject?,
  public val rootProject: RootProject,
  public val includedBuilds: List<RootProject> = emptyList(),
  public val subprojects: List<Subproject> = emptyList(),
) {

  public enum class DslKind {
    GROOVY,
    KOTLIN
  }

  public fun writer(): GradleProjectWriter = GradleProjectWriter(this)
  public fun write(): GradleProject {
    writer().write()
    return this
  }

  /**
   * Use ":" for the root project.
   */
  public fun projectDir(projectName: String): Path {
    return projectDir(forName(projectName))
  }

  /**
   * Use [rootProject] for the root project.
   */
  public fun projectDir(project: Subproject): Path {
    if (project == rootProject) {
      return rootDir.toPath()
    }
    return rootDir.toPath().resolve("${project.includedBuild?.let { "$it/" } ?: ""}${project.name.replace(":", "/")}/")
  }

  /**
   * Use ":" for the root project.
   */
  public fun buildDir(projectName: String): Path {
    return buildDir(forName(projectName))
  }

  /**
   * Use [rootProject] for the root project.
   */
  public fun buildDir(project: Subproject): Path {
    return projectDir(project).resolve("build/")
  }

  private fun forName(projectName: String): Subproject {
    if (projectName == ":") {
      return rootProject
    }

    return subprojects.find { it.name == projectName }
      ?: throw IllegalStateException("No subproject with name $projectName")
  }

  public class Builder @JvmOverloads constructor(
    private val rootDir: File,
    private val dslKind: DslKind = DslKind.GROOVY,
  ) {
    private var buildSrcBuilder: Subproject.Builder? = null
    private var rootProjectBuilder: RootProject.Builder = defaultRootProjectBuilder()
    private var includedProjectMap: MutableMap<String, RootProject.Builder> = mutableMapOf()
    private val subprojectMap: MutableMap<String, Subproject.Builder> = mutableMapOf()
    private val androidSubprojectMap: MutableMap<String, AndroidSubproject.Builder> = mutableMapOf()

    public fun withBuildSrc(block: Subproject.Builder.() -> Unit): Builder {
      val builder = Subproject.Builder()
      builder.apply {
        this.name = "buildSrc"
        block(this)
      }
      buildSrcBuilder = builder

      return this
    }

    public fun withRootProject(block: RootProject.Builder.() -> Unit): Builder {
      rootProjectBuilder = rootProjectBuilder.apply {
        block(this)
      }

      return this
    }

    public fun withIncludedBuild(name: String, block: RootProject.Builder.() -> Unit): Builder {
      // If a builder with this name already exists, returning it for building-upon
      val builder = includedProjectMap[name] ?: defaultRootProjectBuilder()
      builder.apply {
        settingsScript = SettingsScript(rootProjectName = name)
        block(this)
      }
      includedProjectMap[name] = builder

      return this
    }

    public fun withSubproject(name: String, block: Subproject.Builder.() -> Unit): Builder {
      val normalizedName = name.removePrefix(":")
      // If a builder with this name already exists, returning it for building-upon
      val builder = subprojectMap[normalizedName] ?: Subproject.Builder()
      builder.apply {
        this.name = normalizedName
        block(this)
      }
      subprojectMap[normalizedName] = builder

      return this
    }

    public fun withSubprojectInIncludedBuild(
      includedBuildName: String,
      includedBuildRootPlugins: List<Plugin>,
      subprojectName: String,
      block: Subproject.Builder.() -> Unit,
    ): Builder {
      val builder = includedProjectMap[includedBuildName] ?: defaultRootProjectBuilder()
      builder.apply {
        settingsScript = SettingsScript(
          rootProjectName = includedBuildName,
          subprojects = settingsScript.subprojects + subprojectName
        )
        withBuildScript {
          plugins += includedBuildRootPlugins
        }
      }
      includedProjectMap[includedBuildName] = builder

      withSubproject(subprojectName) {
        this.includedBuild = includedBuildName
        block(this)
      }

      return this
    }

    public fun withAndroidSubproject(name: String, block: AndroidSubproject.Builder.() -> Unit): Builder {
      // If a builder with this name already exists, returning it for building-upon
      val builder = androidSubprojectMap[name] ?: AndroidSubproject.Builder()
      builder.apply {
        this.name = name
        block(this)
      }
      androidSubprojectMap[name] = builder

      return this
    }

    public fun withAndroidLibProject(
      name: String,
      packageName: String,
      block: AndroidSubproject.Builder.() -> Unit,
    ): Builder {
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

      return this
    }

    private fun defaultRootProjectBuilder(): RootProject.Builder {
      return RootProject.Builder().apply {
        variant = ":"
        gradleProperties = GradleProperties.minimalJvmProperties()
        settingsScript = SettingsScript()
        buildScript = BuildScript()
        sources = emptyList()
      }
    }

    public fun build(): GradleProject {
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

    /**
     * Builds this [builder][GradleProject.Builder] and then writes it to disk. Returns the final [GradleProject].
     */
    public fun write(): GradleProject {
      val project = build()
      return project.write()
    }
  }
}
