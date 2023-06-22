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
        bs.additions = """\
          group = 'first'
          version = '1.0'
        """.stripIndent()
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
        bs.plugins.add(Plugin.javaLibraryPlugin)
        bs.dependencies = [new Dependency('testImplementation', 'first:the-project:1.0')]
        bs.additions = """\
          group = 'second'
          version = '1.0'
        """.stripIndent()
      }
      second.settingsScript.additions = """\
        includeBuild('..') { name = 'the-project' }
      """.stripIndent()
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

  Set<ProjectAdvice> actualBuildHealthOfSecondBuild() {
    def included = gradleProject.includedBuilds[0]
    def project = new GradleProject(new java.io.File(gradleProject.rootDir, 'second-build'), null, included, [], [])
    return actualProjectAdvice(project)
  }

  static Set<ProjectAdvice> expectedBuildHealth(String buildNameInAdvice) {[
    projectAdviceForDependencies(':', [
      Advice.ofRemove(
        includedBuildCoordinates('second:second-build', projectCoordinates(':', 'second:second-build', buildNameInAdvice)),
        'implementation'
      )
    ] as Set<Advice>)
  ]}

  static Set<ProjectAdvice> expectedBuildHealthOfIncludedBuild(String buildNameInAdvice) {[
    projectAdviceForDependencies(':', [
      Advice.ofRemove(
        includedBuildCoordinates('first:the-project', projectCoordinates(':', 'first:the-project', buildNameInAdvice)),
        'testImplementation'
      )
    ] as Set<Advice>)
  ]}
}
