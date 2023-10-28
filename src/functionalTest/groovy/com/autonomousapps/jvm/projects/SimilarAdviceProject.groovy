package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.gradle.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

final class SimilarAdviceProject extends AbstractProject {

  final GradleProject gradleProject

  SimilarAdviceProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('lib1') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = []
      }
    }
    builder.withSubproject('lib2') { s ->
      s.sources = sources
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibrary]
        bs.dependencies = []
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private List<Source> sources = [
    new Source(
      SourceType.JAVA, "Main", "com/example",
      """\
        package com.example;
        
        public class Main {
          public int magic() {
            return 42;
          }
        }""".stripIndent()
    )
  ]
}
