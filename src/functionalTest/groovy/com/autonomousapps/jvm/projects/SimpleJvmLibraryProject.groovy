package com.autonomousapps.jvm.projects

import com.autonomousapps.AbstractProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Plugin
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.SourceType

/**
 * This project has the `java-library` plugin applied. We are only testing to see if `assemble` also
 * executes advice tasks (it shouldn't).
 */
final class SimpleJvmLibraryProject extends AbstractProject {

  final GradleProject gradleProject

  SimpleJvmLibraryProject() {
    this.gradleProject = build()
  }

  private GradleProject build() {
    def builder = newGradleProjectBuilder()
    builder.withSubproject('proj') { s ->
      s.sources = [JAVA_SOURCE]
      s.withBuildScript { bs ->
        bs.plugins = [Plugin.javaLibraryPlugin]
      }
    }

    def project = builder.build()
    project.writer().write()
    return project
  }

  private static final Source JAVA_SOURCE = new Source(
    SourceType.JAVA, "Main", "com/example",
    """\
      package com.example;
      
      public class Main {
        public static void main(String... args) {
        }
      }""".stripIndent()
  )
}
