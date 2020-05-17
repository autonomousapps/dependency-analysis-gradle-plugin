package com.autonomousapps.jvm.projects

import com.autonomousapps.kit.*

final class RenamingProject {

  private static final COMMONS_IO = 'commons-io:commons-io:2.6'
  private static final COMMONS_IO_RENAMED = 'commons-io'

  final GradleProject gradleProject

  RenamingProject() {
    String additions =
      "def renamingMap = new HashMap()\n" +
        "renamingMap.put('${COMMONS_IO}', '${COMMONS_IO_RENAMED}')\n" +
        "dependencyAnalysis.dependencyRenamingMap = renamingMap"
    gradleProject = build(additions)
  }

  @SuppressWarnings("GrMethodMayBeStatic")
  String expectedRenamedConsoleReport() {
    return "Unused dependencies which should be removed:\n" +
      "- implementation(${COMMONS_IO_RENAMED})"
  }

  private static GradleProject build(String additions) {
    def builder = new GradleProject.Builder()

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
    def source = new Source(SourceType.KOTLIN, 'Library', 'com/example', sourceCode)
    builder.addSubproject(plugins, dependencies, [source], 'main', '')

    def project = builder.build()
    project.writer().write()
    return project
  }
}
