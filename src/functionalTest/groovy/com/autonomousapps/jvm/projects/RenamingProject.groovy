package com.autonomousapps.jvm.projects

import com.autonomousapps.fixtures.jvm.Dependency
import com.autonomousapps.fixtures.jvm.JvmProject
import com.autonomousapps.fixtures.jvm.Plugin
import com.autonomousapps.fixtures.jvm.SourceType

final class RenamingProject {

  private static final COMMONS_IO = 'commons-io:commons-io:2.6'
  private static final COMMONS_IO_RENAMED = 'commons-io'

  final JvmProject jvmProject

  RenamingProject() {
    String additions =
      "def renamingMap = new HashMap()\n" +
        "renamingMap.put('${COMMONS_IO}', '${COMMONS_IO_RENAMED}')\n" +
        "dependencyAnalysis.dependencyRenamingMap = renamingMap"
    jvmProject = build(additions)
  }

  static String expectedRenamedConsoleReport() {
    return "Unused dependencies which should be removed:\n" +
      "- implementation(${COMMONS_IO_RENAMED})"
  }

  private static JvmProject build(String additions) {
    def builder = new JvmProject.Builder()

    builder.rootAdditions = additions

    def plugins = [Plugin.kotlinPlugin(true, null)]
    def dependencies = [
      Dependency.kotlinStdlibJdk7('implementation'),
      new Dependency('implementation', 'commons-io:commons-io:2.6')
    ]
    def sourceCode = """\
      package com.example
      
      class Library {
        fun magic() = 42
      }
    """.stripIndent()
    def source = new com.autonomousapps.fixtures.jvm.Source(SourceType.KOTLIN, 'Library', 'com/example', sourceCode)
    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }
}
