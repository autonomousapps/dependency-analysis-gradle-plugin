package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsIO
import static com.autonomousapps.kit.gradle.dependencies.Dependencies.commonsMath

final class DoubleExclusionsProject extends AbstractProject {

  private final javaLibrary = [Plugin.javaLibrary]

  final GradleProject gradleProject

  DoubleExclusionsProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = javaLibrary
        bs.dependencies = [commonsIO('implementation'), commonsMath('implementation')]
        bs.withGroovy("""\
          dependencyAnalysis {
            issues { 
              onUnusedDependencies {
                exclude("commons-io:commons-io")
              }
            }
          }
          dependencyAnalysis {
            issues{
              onUnusedDependencies {
                exclude("org.apache.commons:commons-math3")
              }
            }
          }""")
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private sources = [
    new Source(
      SourceType.JAVA, 'Main', 'com/example',
      """\
        package com.example;
       
        public class Main {
          public Main() {}
        
          public void hello() {
            System.out.println("hello");
          }
        }""".stripIndent()
    )
  ]

  Set<ProjectAdvice> actualProjectAdvice() {
    return actualProjectAdvice(gradleProject)
  }
}
