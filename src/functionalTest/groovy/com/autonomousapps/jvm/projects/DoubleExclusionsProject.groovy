package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType
import com.autonomousapps.model.ProjectAdvice

import static com.autonomousapps.AdviceHelper.actualProjectAdvice
import static com.autonomousapps.kit.gradle.Dependency.commonsIO
import static com.autonomousapps.kit.gradle.Dependency.commonsMath

final class DoubleExclusionsProject extends AbstractProject {

    private final javaLibrary = [Plugin.javaLibraryPlugin]

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
                bs.additions = """\
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
          }""".stripIndent()
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
