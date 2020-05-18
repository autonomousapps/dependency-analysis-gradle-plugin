package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*

final class RenamingProject extends AbstractProject {

  private static final COMMONS_IO = 'commons-io:commons-io:2.6'
  private static final COMMONS_IO_RENAMED = 'commons-io'

  final GradleProject gradleProject

  RenamingProject() {
    gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { r ->
      r.withBuildScript { bs ->
        bs.additions = additions
      }
    }
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = plugins
        bs.dependencies = dependencies
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private final String additions =
    "def renamingMap = new HashMap()\n" +
      "renamingMap.put('${COMMONS_IO}', '${COMMONS_IO_RENAMED}')\n" +
      "dependencyAnalysis.dependencyRenamingMap = renamingMap"

  private final List<Plugin> plugins = [Plugin.kotlinPlugin(null, true)]
  private final List<Dependency> dependencies = [
    Dependency.kotlinStdlibJdk7('implementation'),
    new Dependency('implementation', 'commons-io:commons-io:2.6')
  ]
  private final List<Source> sources = [
    new Source(
      SourceType.KOTLIN,
      'Library',
      'com/example',
      """\
        package com.example
        
        class Library {
          fun magic() = 42
        }
      """.stripIndent()
    )
  ]

  @SuppressWarnings("GrMethodMayBeStatic")
  String expectedRenamedConsoleReport() {
    return "Unused dependencies which should be removed:\n" +
      "- implementation(${COMMONS_IO_RENAMED})"
  }
}
