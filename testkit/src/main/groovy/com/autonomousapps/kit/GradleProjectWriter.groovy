package com.autonomousapps.kit

import java.nio.file.Path

final class GradleProjectWriter {

  private final GradleProject gradleProject

  GradleProjectWriter(GradleProject gradleProject) {
    this.gradleProject = gradleProject
  }

  void write() {
    def rootDir = gradleProject.rootDir
    rootDir.mkdirs()

    Path rootPath = rootDir.toPath()

    // gradle.properties
    Path gradleProperties = rootPath.resolve('gradle.properties')
    gradleProperties.toFile().write(gradleProject.rootProject.gradleProperties.toString())

    // Settings script
    Path settingsFile = rootPath.resolve('settings.gradle')
    settingsFile.toFile().write(gradleProject.rootProject.settingScript.toString())

    // Root build script
    Path rootBuildScript = rootPath.resolve('build.gradle')
    rootBuildScript.toFile().write(gradleProject.rootProject.buildScript.toString())

    // (Optional) Source
    gradleProject.rootProject.sources.forEach { source ->
      Path sourceRootPath = rootPath.resolve("src/main/${source.sourceType.value}")
      Path sourcePath = sourceRootPath.resolve(source.path)
      sourcePath.toFile().mkdirs()
      Path filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

      filePath.toFile().write(source.source)
    }

    // (Optional) Subprojects
    gradleProject.subprojects.each { subproject ->
      if (subproject instanceof AndroidSubproject) {
        new AndroidSubprojectWriter(rootPath, subproject).write()
      } else {
        new SubprojectWriter(rootPath, subproject).write()
      }
    }
  }

  private static class SubprojectWriter {

    private final Path rootPath
    private final Subproject subproject

    protected Path projectPath

    SubprojectWriter(Path rootPath, Subproject subproject) {
      this.rootPath = rootPath
      this.subproject = subproject

      projectPath = rootPath.resolve(subproject.name)
      projectPath.toFile().mkdirs()
    }

    void write() {
      // Build script
      Path buildScriptPath = projectPath.resolve('build.gradle')
      buildScriptPath.toFile().write(subproject.buildScript.toString())

      // Sources
      subproject.sources.each { source ->
        Path sourceRootPath = projectPath.resolve("src/main/${source.sourceType.value}")
        Path sourcePath = sourceRootPath.resolve(source.path)
        sourcePath.toFile().mkdirs()
        Path filePath = sourcePath.resolve("${source.name}.${source.sourceType.fileExtension}")

        filePath.toFile().write(source.source)
      }
    }
  }

  private static final class AndroidSubprojectWriter extends SubprojectWriter {

    private final AndroidSubproject androidSubproject

    AndroidSubprojectWriter(Path rootPath, AndroidSubproject androidSubproject) {
      super(rootPath, androidSubproject)
      this.androidSubproject = androidSubproject
    }

    void write() {
      super.write()

      def manifestPath = projectPath.resolve('src/main/AndroidManifest.xml')
      manifestPath.parent.toFile().mkdirs()
      manifestPath.toFile().write(androidSubproject.manifest.toString())

      def stylesPath = projectPath.resolve('src/main/res/values/styles.xml')
      stylesPath.parent.toFile().mkdirs()
      stylesPath.toFile().write(androidSubproject.styles.toString())

      def colorsPath = projectPath.resolve('src/main/res/values/colors.xml')
      colorsPath.parent.toFile().mkdirs()
      colorsPath.toFile().write(androidSubproject.colors.toString())

      def layoutsPath = projectPath.resolve('src/main/res/layout/')
      layoutsPath.toFile().mkdirs()
      androidSubproject.layouts.forEach { layout ->
        def layoutPath = layoutsPath.resolve(layout.filename)
        layoutPath.toFile().write(layout.content)
      }
    }
  }
}
