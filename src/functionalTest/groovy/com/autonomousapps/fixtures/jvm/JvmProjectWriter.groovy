package com.autonomousapps.fixtures.jvm

import java.nio.file.Path

final class JvmProjectWriter {

  private final JvmProject jvmProject

  JvmProjectWriter(JvmProject jvmProject) {
    this.jvmProject = jvmProject
  }

  void write() {
    def rootDir = jvmProject.rootDir
    rootDir.mkdirs()

    Path rootPath = rootDir.toPath()

    // Settings script
    Path settingsFile = rootPath.resolve('settings.gradle')
    settingsFile.toFile().write(jvmProject.rootProject.settingScript.toString())

    // Root build script
    Path rootBuildScript = rootPath.resolve('build.gradle')
    rootBuildScript.toFile().write(jvmProject.rootProject.buildScript.toString())

    // (Optional) Source
    // TODO

    // (Optional) Subprojects
    jvmProject.subprojects.each { subproject ->
      Path projectPath = rootPath.resolve(subproject.name)
      projectPath.toFile().mkdirs()

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
}
