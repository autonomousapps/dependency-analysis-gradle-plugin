package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*

final class IncludedBuildProject extends AbstractProject {

  final GradleProject gradleProject

  IncludedBuildProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.withBuildScript { bs ->
        bs.plugins.add(Plugin.javaLibraryPlugin)
        bs.dependencies = [new Dependency('implementation', 'second:second-build:1.0')]
      }
      root.settingsScript.additions = """\
        includeBuild 'second-build'
      """.stripIndent()
      root.sources = [
        new Source(
          SourceType.JAVA, 'Main', 'com/example/main',
          """\
            package com.example.main;
                        
            public class Main {}
          """.stripIndent()
        )
      ]
    }
    builder.withIncludedBuild('second-build') { second ->
      second.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.additions = """\
          group = 'second'
          version = '1.0'
        """.stripIndent()
      }
      second.sources = [
        new Source(
          SourceType.JAVA, 'Second', 'com/example/included',
          """\
            package com.example.included;
                        
            public class Second {}
          """.stripIndent()
        )
      ]
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':', [
      Advice.ofRemove(
        includedBuildCoordinates('second:second-build', '1.0', projectCoordinates(':second-build')),
        'implementation'
      )
    ] as Set<Advice>)
  ]
}
