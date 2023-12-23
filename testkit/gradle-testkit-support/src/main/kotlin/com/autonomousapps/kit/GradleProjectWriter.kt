package com.autonomousapps.kit

import com.autonomousapps.kit.android.AndroidSubproject
import com.autonomousapps.kit.internal.writeAny
import com.autonomousapps.kit.render.Scribe
import java.nio.file.Path

public class GradleProjectWriter(private val gradleProject: GradleProject) {

  public fun write() {
    val rootPath = gradleProject.rootDir.run {
      mkdirs()
      toPath()
    }

    RootProjectWriter(rootPath, gradleProject.rootProject, gradleProject.dslKind).write()

    // (Optional) buildSrc
    gradleProject.buildSrc?.let { buildSrc ->
      SubprojectWriter(rootPath, gradleProject.dslKind, buildSrc).write()
    }

    // (Optional) Included builds
    gradleProject.includedBuilds.forEach { includedBuild ->
      includedBuild.writer().write()
    }

    // (Optional) Subprojects
    gradleProject.subprojects.forEach { subproject ->
      if (subproject is AndroidSubproject) {
        AndroidSubprojectWriter(rootPath, gradleProject.dslKind, subproject).write()
      } else {
        SubprojectWriter(rootPath, gradleProject.dslKind, subproject).write()
      }
    }
  }

  private class RootProjectWriter(
    private val rootPath: Path,
    private val rootProject: RootProject,
    private val dslKind: GradleProject.DslKind,
  ) {

    private val scribe = Scribe(
      dslKind = dslKind,
      indent = 2,
    )

    fun write() {
      // gradle.properties
      val gradleProperties = rootPath.resolve("gradle.properties")
      gradleProperties.toFile().writeText(rootProject.gradleProperties.toString())

      // Settings script
      val settingsFileName = dslKind.settingsFile
      val settingsFile = rootPath.resolve(settingsFileName)
      settingsFile.toFile().writeText(rootProject.settingsScript.render(scribe))

      // Root build script
      val buildFileName = dslKind.buildFile
      val rootBuildScript = rootPath.resolve(buildFileName)
      rootBuildScript.toFile().writeText(rootProject.buildScript.render(scribe))

      // (Optional) arbitrary files
      rootProject.files.forEach { file ->
        val filePath = rootPath.resolve(file.path)
        filePath.parent.toFile().mkdirs()
        filePath.toFile().writeText(file.content)
      }

      // (Optional) Source
      rootProject.sources.forEach { source ->
        SourceWriter(rootPath, source).write()
      }
    }
  }

  private class SourceWriter(private val rootPath: Path, private val source: Source) {
    fun write() {
      if (source.path.isNotEmpty() && !source.source.contains("package")) {
        throw IllegalStateException("Source does not contain a package declaration. Did you forget it?")
      }

      val sourceRootPath = rootPath.resolve(source.rootPath())
      val sourcePath = sourceRootPath.resolve(source.path)
      sourcePath.toFile().mkdirs()
      val filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

      filePath.toFile().writeAny(source)
    }
  }

  private open class SubprojectWriter(
    rootPath: Path,
    private val dslKind: GradleProject.DslKind,
    private val subproject: Subproject,
  ) {

    protected val projectPath: Path = rootPath.resolve(
      "${subproject.includedBuild?.let { "$it/" } ?: ""}${subproject.name.replace(":", "/")}"
    ).also {
      it.toFile().mkdirs()
    }

    open fun write() {
      // Build script
      val fileName = if (dslKind == GradleProject.DslKind.GROOVY) "build.gradle" else "build.gradle.kts"
      val buildScriptPath = projectPath.resolve(fileName)
      buildScriptPath.toFile().writeText(subproject.buildScript.render(Scribe(dslKind, 2)))

      // Sources
      subproject.sources.forEach { source ->
        SourceWriter(projectPath, source).write()
      }

      // (Optional) arbitrary files
      subproject.files.forEach { file ->
        val filePath = projectPath.resolve(file.path)
        filePath.parent.toFile().mkdirs()
        filePath.toFile().writeText(file.content)
      }
    }
  }

  private class AndroidSubprojectWriter(
    rootPath: Path,
    dslKind: GradleProject.DslKind,
    private val androidSubproject: AndroidSubproject,
  ) : SubprojectWriter(rootPath, dslKind, androidSubproject) {

    override fun write() {
      super.write()

      androidSubproject.manifest?.let { manifest ->
        val manifestPath = projectPath.resolve("src/main/AndroidManifest.xml")
        manifestPath.parent.toFile().mkdirs()
        manifestPath.toFile().writeText(manifest.toString())
      }

      androidSubproject.styles?.let { styles ->
        if (!styles.isBlank()) {
          val stylesPath = projectPath.resolve("src/main/res/values/styles.xml")
          stylesPath.parent.toFile().mkdirs()
          stylesPath.toFile().writeText(styles.toString())
        }
      }

      androidSubproject.strings?.let { strings ->
        if (!strings.isBlank()) {
          val stringsPath = projectPath.resolve("src/main/res/values/strings.xml")
          stringsPath.parent.toFile().mkdirs()
          stringsPath.toFile().writeText(strings.toString())
        }
      }

      androidSubproject.colors?.let { colors ->
        if (!colors.isBlank()) {
          val colorsPath = projectPath.resolve("src/main/res/values/colors.xml")
          colorsPath.parent.toFile().mkdirs()
          colorsPath.toFile().writeText(colors.toString())
        }
      }

      androidSubproject.layouts?.let { layouts ->
        if (layouts.isNotEmpty()) {
          val layoutsPath = projectPath.resolve("src/main/res/layout/")
          layoutsPath.toFile().mkdirs()
          layouts.forEach { layout ->
            val layoutPath = layoutsPath.resolve(layout.filename)
            layoutPath.toFile().writeText(layout.content)
          }
        }
      }
    }
  }
}
