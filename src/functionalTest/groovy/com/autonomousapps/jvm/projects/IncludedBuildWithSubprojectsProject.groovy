package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.*
import com.autonomousapps.model.Advice
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.*

final class IncludedBuildWithSubprojectsProject extends AbstractProject {

  final GradleProject gradleProject
  final boolean useProjectDependencyWherePossible

  IncludedBuildWithSubprojectsProject(boolean useProjectDependencyWherePossible = false) {
    this.useProjectDependencyWherePossible = useProjectDependencyWherePossible
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withRootProject { root ->
      root.withBuildScript { bs ->
        bs.plugins.add(Plugin.javaLibraryPlugin)
        bs.dependencies = [new Dependency('implementation', 'second:second-sub2:does-not-matter')]
      }
      root.settingsScript.additions = """\
        includeBuild 'second-build'
      """.stripIndent()
      root.sources = [
        new Source(
          SourceType.JAVA, 'Main', 'com/example/main',
          """\
            package com.example.main;
      
            import com.example.included.sub2.SecondSub2;
      
            public class Main {
              SecondSub2 sub2 = new SecondSub2();
            }
          """.stripIndent()
        )
      ]
    }
    builder.withSubprojectInIncludedBuild('second-build', 'second-sub1') { secondSub ->
      secondSub.withBuildScript { bs ->
        bs.plugins.add(Plugin.javaLibraryPlugin)
        if (useProjectDependencyWherePossible) {
          bs.dependencies = [new Dependency('api', ':second-sub2')]
        } else {
          bs.dependencies = [new Dependency('api', 'second:second-sub2')]
        }
        bs.additions = """\
          group = 'second'
        """.stripIndent()
      }
      secondSub.sources = [
        new Source(
          SourceType.JAVA, 'SecondSub1', 'com/example/included/sub',
          """\
            package com.example.included.sub1;
                        
            import com.example.included.sub2.SecondSub2;
            
            public class SecondSub1 {
              SecondSub2 sub2 = new SecondSub2();
            }
          """.stripIndent()
        )
      ]
    }
    builder.withSubprojectInIncludedBuild('second-build', 'second-sub2') { secondSub ->
      secondSub.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
        bs.additions = """\
          group = 'second'
        """.stripIndent()
      }
      secondSub.sources = [
        new Source(
          SourceType.JAVA, 'SecondSub2', 'com/example/included/sub',
          """\
            package com.example.included.sub2;
                        
            public class SecondSub2 {}
          """.stripIndent()
        )
      ]
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  // Health of the root build (the including one)
  Set<ProjectAdvice> actualBuildHealth() {
    return actualProjectAdvice(gradleProject)
  }

  final Set<ProjectAdvice> expectedBuildHealth = [
    projectAdviceForDependencies(':', [] as Set<Advice>)
  ]

  // Health of the included build
  Set<ProjectAdvice> actualIncludedBuildHealth() {
    def included = gradleProject.includedBuilds[0]
    def project = new GradleProject(new java.io.File(gradleProject.rootDir, 'second-build'), null, included, [], [])
    return actualProjectAdvice(project)
  }

  final Set<ProjectAdvice> expectedIncludedBuildHealth(String buildPathInAdvice) {[
    projectAdviceForDependencies(':second-sub1', [
      useProjectDependencyWherePossible
        ? Advice.ofChange(projectCoordinates(':second-sub2', null, buildPathInAdvice), 'api', 'implementation')
        : Advice.ofChange(includedBuildCoordinates('second:second-sub2', projectCoordinates(':second-sub2', 'second:second-sub2', buildPathInAdvice)), 'api', 'implementation')
    ] as Set<Advice>),
    projectAdviceForDependencies(':second-sub2', [] as Set<Advice>)
  ]}
}
