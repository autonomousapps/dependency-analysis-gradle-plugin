package com.autonomousapps.kit

import com.autonomousapps.kit.internal.writeAny
import java.nio.file.Path
import kotlin.io.path.createDirectories

class GradleProjectWriter(private val gradleProject: GradleProject) {

  fun write() {
    val rootPath = gradleProject.rootDir.run {
      mkdirs()
      toPath()
    }

    RootProjectWriter(rootPath, gradleProject.rootProject).write()

    // (Optional) buildSrc
    gradleProject.buildSrc?.let { buildSrc ->
      SubprojectWriter(rootPath, buildSrc).write()
    }

    // (Optional) Included builds
    gradleProject.includedBuilds.forEach { includedBuild ->
      val path = includedBuild.settingsScript.rootProjectName.run {
        rootPath.resolve(this).createDirectories()
      }
      RootProjectWriter(path, includedBuild).write()
    }

    // (Optional) Subprojects
    gradleProject.subprojects.forEach { subproject ->
      if (subproject is AndroidSubproject) {
        AndroidSubprojectWriter(rootPath, subproject).write()
      } else {
        SubprojectWriter(rootPath, subproject).write()
      }
    }
  }

  private class RootProjectWriter(private val rootPath: Path, private val rootProject: RootProject) {
    fun write() {
      // gradle.properties
      val gradleProperties = rootPath.resolve("gradle.properties")
      gradleProperties.toFile().writeText(rootProject.gradleProperties.toString())

      // Settings script
      val settingsFile = rootPath.resolve("settings.gradle")
      settingsFile.toFile().writeText(rootProject.settingsScript.toString())

      // Root build script
      val rootBuildScript = rootPath.resolve("build.gradle")
      rootBuildScript.toFile().writeText(rootProject.buildScript.toString())

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

      val sourceRootPath = rootPath.resolve("src/${source.sourceSet}/${source.sourceType.value}")
      val sourcePath = sourceRootPath.resolve(source.path)
      sourcePath.toFile().mkdirs()
      val filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

      filePath.toFile().writeAny(source)
    }
  }

  private open class SubprojectWriter(
    rootPath: Path,
    private val subproject: Subproject
  ) {

    protected val projectPath: Path = rootPath.resolve(subproject.name).also {
      it.toFile().mkdirs()
    }

    open fun write() {
      // Build script
      val buildScriptPath = projectPath.resolve("build.gradle")
      buildScriptPath.toFile().writeText(subproject.buildScript.toString())

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
    private val androidSubproject: AndroidSubproject
  ) : SubprojectWriter(rootPath, androidSubproject) {

    override fun write() {
      super.write()

      androidSubproject.manifest?.let { manifest ->
        val manifestPath = projectPath.resolve("src/main/AndroidManifest.xml")
        manifestPath.parent.toFile().mkdirs()
        manifestPath.toFile().writeText(manifest.toString())
      }

      androidSubproject.styles?.let { styles ->
        val stylesPath = projectPath.resolve("src/main/res/values/styles.xml")
        stylesPath.parent.toFile().mkdirs()
        stylesPath.toFile().writeText(styles.toString())
      }

      androidSubproject.strings?.let { strings ->
        val stringsPath = projectPath.resolve("src/main/res/values/strings.xml")
        stringsPath.parent.toFile().mkdirs()
        stringsPath.toFile().writeText(strings.toString())
      }

      androidSubproject.colors?.let { colors ->
        val colorsPath = projectPath.resolve("src/main/res/values/colors.xml")
        colorsPath.parent.toFile().mkdirs()
        colorsPath.toFile().writeText(colors.toString())
      }

      androidSubproject.layouts?.let { layouts ->
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
